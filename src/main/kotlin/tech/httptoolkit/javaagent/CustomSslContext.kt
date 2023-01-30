package tech.httptoolkit.javaagent

import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

fun buildTrustManagerFactoryForCertificate(certPath: String): TrustManagerFactory {
    val certFile = File(certPath)
    val certificates = CertificateFactory.getInstance("X.509")
        .generateCertificates(FileInputStream(certFile))

    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    for (certificate in certificates) {
        keyStore.setCertificateEntry(UUID.randomUUID().toString(), certificate)
    }

    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(keyStore)
    return trustManagerFactory
}

fun buildSslContextForCertificate(trustManagerFactory: TrustManagerFactory): SSLContext {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagerFactory.trustManagers, null)
    return sslContext
}