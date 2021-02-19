package tech.httptoolkit.javaagent

import java.lang.IllegalArgumentException
import java.lang.Integer.parseInt
import java.net.URI
import java.net.URISyntaxException

data class Config(
    val certPath: String,
    val proxyHost: String,
    val proxyPort: Int
)

fun getConfigFromEnv(): Config {
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

fun formatConfigArg(proxyHost: String, proxyPort: String, certPath: String): String {
    return "$proxyHost|$proxyPort|$certPath"
}

fun getConfigFromArg(arg: String): Config {
    val (proxyHost, proxyPort, certPath) = arg.split("|", limit = 3)
    // ^ Limited so that you can use | in filenames, if you *really* want to be difficult
    return Config(certPath, proxyHost, parseInt(proxyPort))
}