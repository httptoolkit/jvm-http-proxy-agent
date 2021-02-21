@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.matcher.ElementMatchers.none
import java.lang.instrument.Instrumentation
import javax.net.ssl.SSLContext
import java.net.*
import javax.net.ssl.HttpsURLConnection


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

fun interceptAllHttps(config: Config, instrumentation: Instrumentation) {
    val (certPath, proxyHost, proxyPort) = config

    InterceptedSslContext = buildSslContextForCertificate(certPath)
    AgentProxyHost = proxyHost
    AgentProxyPort = proxyPort

    // Reconfigure the JVM default settings:
    setDefaultProxy(proxyHost, proxyPort)
    setDefaultSslContext(InterceptedSslContext)

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