package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*

// Ensures that the proxy is trusted by setting the Vert'x TrustOptions based on the TrustManager
// created by the agent
class VertxNetClientOptionsTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("io.vertx.core.net.NetClientOptions")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.vertxclient.VertxNetClientOptionsSetTrustOptionsAdvice")
                .on(
                    isConstructor<MethodDescription>()
                        .and(takesArguments(1))
                        .and(takesArgument(0, named("io.vertx.core.net.ClientOptionsBase")))))
    }
}
