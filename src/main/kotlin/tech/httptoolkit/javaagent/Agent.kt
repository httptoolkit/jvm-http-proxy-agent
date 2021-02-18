@file:JvmName("HttpProxyAgent")

package tech.httptoolkit.javaagent

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

    println("HTTP Toolkit interception active")
}

private fun setDefaultProxy(proxyHost: String, proxyPort: Int) {
    System.setProperty("http.proxyHost", proxyHost)
    System.setProperty("http.proxyPort", proxyPort.toString())
    System.setProperty("https.proxyHost", proxyHost)
    System.setProperty("https.proxyPort", proxyPort.toString())
}