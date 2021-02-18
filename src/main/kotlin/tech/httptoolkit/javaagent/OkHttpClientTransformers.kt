package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.utility.JavaModule
import java.net.InetSocketAddress
import java.net.Proxy
import javax.net.ssl.SSLContext

/**
 * Transforms the OkHttpClient for v3 & 4 to use our proxy & trust our certificate.
 *
 * We do that by overwriting the proxy & sslSocketFactory properties on all OkHttp
 * clients to always return our proxy & a socket factory that only trusts our
 * certificate, ignoring anything the application has configured or defaulted to.
 *
 * Without this, proxy settings work by default, but certificates do not - OkHttp
 * only trusts the default built-in certificates, and refuses ours.
 */
class OkHttpClientTransformer(
    private val proxyHost: String,
    private val proxyPort: Int,
    private val sslContext: SSLContext
) : AgentBuilder.Transformer {
    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        val proxyAddress = InetSocketAddress(proxyHost, proxyPort)
        val proxy = Proxy(Proxy.Type.HTTP, proxyAddress)

        return builder
            // v3 uses proxy() functions, while v4 uses Kotlin getters that compile to the same thing
            .method(ElementMatchers.named("proxy")).intercept(FixedValue.value(proxy))
            .method(ElementMatchers.named("sslSocketFactory")).intercept(FixedValue.value(sslContext.socketFactory))
    }
}

/**
 * Transforms the OkHttpClient for v2 to use our proxy & trust our certificate.
 *
 * We do that by overwriting the proxy & sslSocketFactory properties on all OkHttp
 * clients to always return our proxy & a socket factory that only trusts our
 * certificate, ignoring anything the application has configured or defaulted to.
 *
 * Without this, proxy settings work by default, but certificates do not - OkHttp
 * only trusts the default built-in certificates, and refuses ours.
 */
class OkHttpClientV2Transformer(
    private val proxyHost: String,
    private val proxyPort: Int,
    private val sslContext: SSLContext
) : AgentBuilder.Transformer {
    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        val proxyAddress = InetSocketAddress(proxyHost, proxyPort)
        val proxy = Proxy(Proxy.Type.HTTP, proxyAddress)

        return builder
            // v2 uses getX methods:
            .method(ElementMatchers.named("getProxy")).intercept(FixedValue.value(proxy))
            .method(ElementMatchers.named("getSslSocketFactory")).intercept(FixedValue.value(sslContext.socketFactory))
    }
}