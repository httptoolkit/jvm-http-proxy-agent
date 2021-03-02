package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.ReturnProxySelectorAdvice
import tech.httptoolkit.javaagent.advice.ReturnSslContextAdvice


class JavaClientTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                hasSuperType(named("java.net.http.HttpClient"))
            ).and(
                not(isInterface())
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReturnProxySelectorAdvice::class.java)
                .on(hasMethodName("proxy")))
            .visit(Advice.to(ReturnSslContextAdvice::class.java)
                .on(hasMethodName("sslContext")))
            .visit(Advice.to(ReturnSslContextAdvice::class.java)
                .on(hasMethodName("theSSLContext")))
    }
}