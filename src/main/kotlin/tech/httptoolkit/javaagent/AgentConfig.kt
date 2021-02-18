package tech.httptoolkit.javaagent

import java.lang.IllegalArgumentException
import java.net.URI
import java.net.URISyntaxException

data class Config(
    val certPath: String,
    val proxyHost: String,
    val proxyPort: Int
)

fun getConfig(): Config {
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