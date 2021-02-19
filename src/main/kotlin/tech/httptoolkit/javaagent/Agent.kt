@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.matcher.ElementMatchers.*
import java.lang.instrument.Instrumentation
import javax.net.ssl.SSLContext
import java.net.*

fun premain(arguments: String?, instrumentation: Instrumentation) {
    val (certPath, proxyHost, proxyPort) = getConfig()

    // Configure the default proxy settings
    setDefaultProxy(proxyHost, proxyPort)
    val proxySelector = ConstantProxySelector(proxyHost, proxyPort)
    ProxySelector.setDefault(proxySelector)

    // Configure the default certificate trust settings
    val sslContext = buildSslContextForCertificate(certPath)
    SSLContext.setDefault(sslContext)

    // Disabling type validation allows us to intercept non-Java types, e.g. Kotlin
    // in OkHttp. See https://github.com/raphw/byte-buddy/issues/764
    var agentBuilder = AgentBuilder.Default(ByteBuddy().with(TypeValidation.DISABLED))
        .with(AgentBuilder.TypeStrategy.Default.REDEFINE)

    mapOf(
        "okhttp3.OkHttpClient" to
                OkHttpClientTransformer(proxyHost, proxyPort, sslContext),
        "com.squareup.okhttp.OkHttpClient" to
                OkHttpClientV2Transformer(proxyHost, proxyPort, sslContext),
        "org.apache.http.conn.ssl.SSLConnectionSocketFactory" to
                ApacheSslSocketFactoryTransformer(),
        "org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory" to
                ApacheSslSocketFactoryTransformer()
    ).forEach { (className, transformer) ->
        agentBuilder = agentBuilder.type(named(className)).transform(transformer)
    }

    agentBuilder = agentBuilder.type(
        hasSuperType(named("org.apache.http.conn.routing.HttpRoutePlanner"))
    ).and(not(isInterface())).transform(
        ApacheClientRoutingV4Transformer(proxyHost, proxyPort)
    )

    agentBuilder = agentBuilder.type(
        hasSuperType(named("org.apache.hc.client5.http.routing.HttpRoutePlanner"))
    ).and(not(isInterface())).transform(
        ApacheClientRoutingV5Transformer(proxyHost, proxyPort)
    )

    agentBuilder.installOn(instrumentation)

    println("HTTP Toolkit interception active")
}

private fun setDefaultProxy(proxyHost: String, proxyPort: Int) {
    System.setProperty("http.proxyHost", proxyHost)
    System.setProperty("http.proxyPort", proxyPort.toString())
    System.setProperty("https.proxyHost", proxyHost)
    System.setProperty("https.proxyPort", proxyPort.toString())
}