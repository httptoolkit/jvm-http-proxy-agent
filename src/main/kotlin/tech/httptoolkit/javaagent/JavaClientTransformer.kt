package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.utility.JavaModule
import net.bytebuddy.matcher.ElementMatchers.*


class JavaClientTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                hasSuperType(named("java.net.http.HttpClient"))
            ).and(
                not(isInterface())
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        return builder
            .visit(Advice.to(ReturnProxySelectorAdvice::class.java)
                .on(hasMethodName("proxy")))
            .visit(Advice.to(ReturnSslContextAdvice::class.java)
                .on(hasMethodName("sslContext")))
            .visit(Advice.to(ReturnSslContextAdvice::class.java)
                .on(hasMethodName("theSSLContext")))
    }
}