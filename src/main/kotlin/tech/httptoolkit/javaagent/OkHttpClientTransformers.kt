package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.ReturnProxyAdvice
import tech.httptoolkit.javaagent.advice.ReturnSslSocketFactoryAdvice

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
class OkHttpClientV3Transformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("okhttp3.OkHttpClient")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            // v3 uses proxy() functions, while v4 uses Kotlin getters that compile to the same thing
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.ReturnProxyAdvice")
                .on(hasMethodName("proxy")))
            // This means we ignore client certs, but that's fine: we can't pass them through the proxy anyway. That
            // needs to be configured separately in the proxy's configuration.
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.ReturnSslSocketFactoryAdvice")
                .on(hasMethodName("sslSocketFactory")))
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
class OkHttpClientV2Transformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("com.squareup.okhttp.OkHttpClient")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            // v2 uses getX methods:
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.ReturnProxyAdvice")
                .on(hasMethodName("getProxy")))
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.ReturnSslSocketFactoryAdvice")
                .on(hasMethodName("getSslSocketFactory")))
    }
}