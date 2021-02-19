package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.named
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
class OkHttpClientV3Transformer(
    private val proxyHost: String,
    private val proxyPort: Int,
    private val sslContext: SSLContext
) : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("okhttp3.OkHttpClient")
            ).transform(this)
    }

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
            .method(named("proxy")).intercept(FixedValue.value(proxy))
            // This means we ignore client certs, but that's fine: we can't pass them through the proxy anyway. That
            // needs to be configured separately in the proxy's configuration.
            .method(named("sslSocketFactory")).intercept(FixedValue.value(sslContext.socketFactory))
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
) : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("com.squareup.okhttp.OkHttpClient")
            ).transform(this)
    }

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
            .method(named("getProxy")).intercept(FixedValue.value(proxy))
            .method(named("getSslSocketFactory")).intercept(FixedValue.value(sslContext.socketFactory))
    }
}