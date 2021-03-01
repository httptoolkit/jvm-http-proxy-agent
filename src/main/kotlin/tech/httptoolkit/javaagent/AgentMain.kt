@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.matcher.ElementMatchers.none
import net.bytebuddy.utility.JavaModule
import java.lang.instrument.Instrumentation
import javax.net.ssl.SSLContext
import java.net.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.TrustManagerFactory


lateinit var InterceptedSslContext: SSLContext
    private set

lateinit var InterceptedTrustManagerFactory: TrustManagerFactory
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

    InterceptedTrustManagerFactory = buildTrustManagerFactoryForCertificate(certPath)
    InterceptedSslContext = buildSslContextForCertificate(InterceptedTrustManagerFactory)
    AgentProxyHost = proxyHost
    AgentProxyPort = proxyPort

    // Reconfigure the JVM default settings:
    setDefaultProxy(proxyHost, proxyPort)
    setDefaultSslContext(InterceptedSslContext)

    val debugMode = !System.getenv("DEBUG_JVM_HTTP_PROXY_AGENT").isNullOrEmpty()
    val logger = TransformationLogger(debugMode)

    // Disabling type validation allows us to intercept non-Java types, e.g. Kotlin
    // in OkHttp. See https://github.com/raphw/byte-buddy/issues/764
    var agentBuilder = AgentBuilder.Default(
            ByteBuddy().with(TypeValidation.DISABLED)
        )
        .ignore(none())
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
        .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
        .disableClassFormatChanges()
        .with(logger)

    arrayOf(
        OkHttpClientV3Transformer(logger),
        OkHttpClientV2Transformer(logger),
        ApacheClientRoutingV4Transformer(logger),
        ApacheClientRoutingV5Transformer(logger),
        ApacheSslSocketFactoryTransformer(logger),
        ApacheClientTlsStrategyTransformer(logger),
        JavaClientTransformer(logger),
        JettyClientTransformer(logger),
        AsyncHttpClientConfigTransformer(logger),
        AsyncHttpChannelManagerTransformer(logger),
        ReactorNettyClientConfigTransformer(logger),
        ReactorNettyProxyProviderTransformer(logger),
        ReactorNettyOverrideRequestAddressTransformer(logger),
        ReactorNettyHttpClientSecureTransformer(logger)
    ).forEach { matchingAgentTransformer ->
        agentBuilder = matchingAgentTransformer.register(agentBuilder)
    }

    agentBuilder.installOn(instrumentation)

    println("HTTP Toolkit interception active")
}

abstract class MatchingAgentTransformer(private val logger: TransformationLogger) : AgentBuilder.Transformer {
    abstract fun register(builder: AgentBuilder): AgentBuilder
    abstract fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*>

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*> {
        logger.beforeTransformation(typeDescription)
        return transform(builder)
    }
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