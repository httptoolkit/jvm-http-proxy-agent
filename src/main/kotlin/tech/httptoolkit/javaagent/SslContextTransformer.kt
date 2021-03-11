package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.SkipMethodAdvice

// We patch SSL context purely to ensure that the default context that we set isn't changed later
// by anybody else. The default context is already set in AgentMain before we begin patching.
class SslContextTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("javax.net.ssl.SSLContext")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            // We set the default SSLContext on startup, before we intercepted anything.
            // Here we patch SSLContext itself so nobody can overwrite that later.
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.SkipMethodAdvice")
                .on(hasMethodName("setDefault")));
    }
}