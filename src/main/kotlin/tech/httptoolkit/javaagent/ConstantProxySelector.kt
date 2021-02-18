package tech.httptoolkit.javaagent

import java.io.IOException
import java.net.*

class ConstantProxySelector(proxyHost: String, proxyPort: Int) : ProxySelector() {

    private val proxyAddress: InetSocketAddress = InetSocketAddress(proxyHost, proxyPort)

    override fun select(uri: URI): MutableList<Proxy> {
        return if (uri.scheme == "http" || uri.scheme == "https") {
            mutableListOf(Proxy(Proxy.Type.HTTP, proxyAddress))
        } else {
            mutableListOf(Proxy.NO_PROXY)
        }
    }

    override fun connectFailed(p0: URI?, p1: SocketAddress?, p2: IOException?) {
        println("Proxy connection failed")
    }

}