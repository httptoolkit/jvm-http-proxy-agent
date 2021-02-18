@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.dynamic.scaffold.TypeValidation
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers.named
import net.bytebuddy.utility.JavaModule
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
        "okhttp3.OkHttpClient" to OkHttpClientTransformer(proxyHost, proxyPort, sslContext),
        "com.squareup.okhttp.OkHttpClient" to OkHttpClientV2Transformer(proxyHost, proxyPort, sslContext)
    ).forEach { (className, transformer) ->
        agentBuilder = agentBuilder.type(named(className)).transform(transformer)
    }

    agentBuilder.installOn(instrumentation)

    println("HTTP Toolkit interception active")
}

private fun setDefaultProxy(proxyHost: String, proxyPort: Int) {
    System.setProperty("http.proxyHost", proxyHost)
    System.setProperty("http.proxyPort", proxyPort.toString())
    System.setProperty("https.proxyHost", proxyHost)
    System.setProperty("https.proxyPort", proxyPort.toString())
}