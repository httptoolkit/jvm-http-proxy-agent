@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.dynamic.scaffold.TypeValidation
import java.lang.instrument.Instrumentation
import javax.net.ssl.SSLContext
import java.net.*

lateinit var interceptedSslContext: SSLContext
    private set

fun premain(arguments: String?, instrumentation: Instrumentation) {
    val (certPath, proxyHost, proxyPort) = getConfig()

    // Configure the default proxy settings
    setDefaultProxy(proxyHost, proxyPort)

    // Configure the default certificate trust settings
    interceptedSslContext = buildSslContextForCertificate(certPath)
    SSLContext.setDefault(interceptedSslContext)

    // Disabling type validation allows us to intercept non-Java types, e.g. Kotlin
    // in OkHttp. See https://github.com/raphw/byte-buddy/issues/764
    var agentBuilder = AgentBuilder.Default(ByteBuddy().with(TypeValidation.DISABLED))
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
        .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())

    arrayOf(
        OkHttpClientV3Transformer(proxyHost, proxyPort, interceptedSslContext),
        OkHttpClientV2Transformer(proxyHost, proxyPort, interceptedSslContext),
        ApacheClientRoutingV4Transformer(proxyHost, proxyPort),
        ApacheClientRoutingV5Transformer(proxyHost, proxyPort),
        ApacheSslSocketFactoryTransformer(),
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
    ProxySelector.setDefault(proxySelector)
}