@file:JvmName("HttpToolkitAgent")

package tech.httptoolkit.server.javaagent

import java.lang.instrument.Instrumentation

import javax.net.ssl.SSLContext

import javax.net.ssl.TrustManagerFactory

import java.security.KeyStore

import java.io.FileInputStream

import java.security.cert.CertificateFactory

import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.*
import java.security.cert.Certificate


fun premain(arguments: String?, instrumentation: Instrumentation) {
    val (certPath, proxyHost, proxyPort) = getConfig()

    forceProxy(proxyHost, proxyPort)
    trustEveryone(certPath)

    println("HTTP Toolkit interception active")
}

data class Config(
    val certPath: String,
    val proxyHost: String,
    val proxyPort: Int
);

private fun getConfig(): Config {
    val proxyUrl: String? = System.getenv("HTTPS_PROXY")
    if (proxyUrl.isNullOrEmpty()) {
        throw IllegalArgumentException("HTTPS interception failed, proxy URL not provided.")
    }

    val parsedProxyUrl = try {
        URI(proxyUrl)
    } catch(parseError: URISyntaxException) {
        throw IllegalArgumentException("HTTPS interception failed, could not parse proxy URL")
    }

    val certPath: String? = System.getenv("SSL_CERT_FILE")

    if (certPath.isNullOrEmpty()) {
        throw IllegalArgumentException("HTTPS interception failed, certificate path not provided.")
    }

    val proxyHost = parsedProxyUrl.host
    val proxyPort = parsedProxyUrl.port

    return Config(certPath, proxyHost, proxyPort)
}

class HtkProxySelector(private val proxyAddress: SocketAddress) : ProxySelector() {

    override fun select(uri: URI): MutableList<Proxy> {
        return if (uri.scheme == "http" || uri.scheme == "https") {
            mutableListOf(Proxy(Proxy.Type.HTTP, proxyAddress));
        } else {
            mutableListOf(Proxy.NO_PROXY)
        }
    }

    override fun connectFailed(p0: URI?, p1: SocketAddress?, p2: IOException?) {
        println("HTTP Toolkit proxy connection failed")
    }

}

private fun forceProxy(proxyHost: String, proxyPort: Int) {
    val proxyAddress = InetSocketAddress(proxyHost, proxyPort);
    ProxySelector.setDefault(HtkProxySelector(proxyAddress))
}

private fun trustEveryone(certPath: String) {
    val crtFile = File(certPath)
    val certificate: Certificate = CertificateFactory.getInstance("X.509").generateCertificate(FileInputStream(crtFile))

    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setCertificateEntry("http-toolkit", certificate)

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagerFactory.trustManagers, null)
    SSLContext.setDefault(sslContext)
}