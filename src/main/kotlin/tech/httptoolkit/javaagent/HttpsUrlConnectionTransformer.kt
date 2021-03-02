package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.ReturnSslSocketFactoryAdvice

// We override the SSLSocketFactory field for HttpsURLConnections. This is the only way to access the
// configured field, so this effectively reconfigured every such connection to trust our certificate.
// Without this, connections still work as our SSLContext is the default, but this ensures they work
// even for connections that are explicitly configured with their own settings.
class HttpsUrlConnectionTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("javax.net.ssl.HttpsURLConnection")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReturnSslSocketFactoryAdvice::class.java)
                .on(hasMethodName("getSSLSocketFactory")))
    }
}