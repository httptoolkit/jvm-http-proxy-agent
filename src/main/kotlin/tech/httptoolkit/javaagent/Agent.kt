@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

import com.sun.tools.attach.VirtualMachine
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.matcher.ElementMatchers.none
import java.lang.instrument.Instrumentation
import javax.net.ssl.SSLContext
import java.net.*
import kotlin.system.exitProcess
import java.lang.management.ManagementFactory
import java.io.File
import javax.net.ssl.HttpsURLConnection
import java.nio.file.Files


lateinit var InterceptedSslContext: SSLContext
    private set

lateinit var AgentProxyHost: String
    private set

var AgentProxyPort = -1
    private set

lateinit var AgentProxySelector: ProxySelector
    private set

// If attached at startup with a -javaagent argument, use either arguments or env
fun premain(arguments: String?, instrumentation: Instrumentation) {
    val config = try {
        getConfigFromArg(arguments!!)
    } catch (e: Throwable) {
        // If that fails for any reason (any kind of parse error at all), try to
        // use our env variables instead
        getConfigFromEnv()
    }
    interceptAllHttps(config, instrumentation)
}

// If attached after startup, pull config from the passed arguments
fun agentmain(arguments: String?, instrumentation: Instrumentation) {
    if (arguments.isNullOrEmpty()) {
        throw Error("Can't attach proxy agent without configuration arguments")
    }
    val config = getConfigFromArg(arguments)
    interceptAllHttps(config, instrumentation)
}

// If run directly, can either list potential targets (list-targets) or attach to a target (pid, ...config)
fun main(args: Array<String>) {
    if (args.size == 1 && args[0] == "list-targets") {
        // This isn't guaranteed to work everywhere, but it should work in most places:
        val (pid) = ManagementFactory.getRuntimeMXBean().name.split("@")

        VirtualMachine.list().forEach { vmd ->
            if (vmd.id() != pid) {
                println("${vmd.id()}:${vmd.displayName()}")
            }
        }
        exitProcess(0)
    } else if (args.size != 4) {
        System.err.println("Usage: java -jar <agent.jar> <target-PID> <proxyHost> <proxyPort> <path-to-certificate>")
        exitProcess(1)
    }

    val (pid, proxyHost, proxyPort, certPath) = args

    val jarPath = File(
        ConstantProxySelector::class.java // Any arbitrary class defined inside this JAR
            .protectionDomain.codeSource.location.path
    ).absolutePath

    // Inject the agent with our config arguments into the target VM
    val vm: VirtualMachine = VirtualMachine.attach(pid)
    vm.loadAgent(jarPath, formatConfigArg(proxyHost, proxyPort, certPath))
    vm.detach()
}

fun interceptAllHttps(config: Config, instrumentation: Instrumentation) {
    val (certPath, proxyHost, proxyPort) = config

    InterceptedSslContext = buildSslContextForCertificate(certPath)
    AgentProxyHost = proxyHost
    AgentProxyPort = proxyPort

    // Reconfigure the JVM default settings:
    setDefaultProxy(proxyHost, proxyPort)
    setDefaultSslContext(InterceptedSslContext)

    val bootstrapCache = Files.createTempDirectory("proxy-agent-bootstrap-cache").toFile()

    // Disabling type validation allows us to intercept non-Java types, e.g. Kotlin
    // in OkHttp. See https://github.com/raphw/byte-buddy/issues/764
    var agentBuilder = AgentBuilder.Default(
            ByteBuddy().with(TypeValidation.DISABLED)
        )
        .ignore(none())
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
        .disableClassFormatChanges()
        .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())

    arrayOf(
        OkHttpClientV3Transformer(),
        OkHttpClientV2Transformer(),
        ApacheClientRoutingV4Transformer(),
        ApacheClientRoutingV5Transformer(),
        ApacheSslSocketFactoryTransformer(),
        JavaClientTransformer(),
    ).forEach { matchingAgentTransformer ->
        agentBuilder = matchingAgentTransformer.register(agentBuilder)
    }

    agentBuilder.installOn(instrumentation)

    println("HTTP Toolkit interception active")
}

interface MatchingAgentTransformer : AgentBuilder.Transformer {
    fun register(builder: AgentBuilder): AgentBuilder
}

private fun setDefaultProxy(proxyHost: String, proxyPort: Int) {
    System.setProperty("http.proxyHost", proxyHost)
    System.setProperty("http.proxyPort", proxyPort.toString())
    System.setProperty("https.proxyHost", proxyHost)
    System.setProperty("https.proxyPort", proxyPort.toString())

    val proxySelector = ConstantProxySelector(proxyHost, proxyPort)
    AgentProxySelector = proxySelector
    ProxySelector.setDefault(proxySelector)
}

private fun setDefaultSslContext(context: SSLContext) {
    SSLContext.setDefault(context)
    HttpsURLConnection.setDefaultSSLSocketFactory(context.socketFactory)
}