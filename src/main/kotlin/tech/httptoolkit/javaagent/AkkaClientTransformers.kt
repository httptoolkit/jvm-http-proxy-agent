package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.hasMethodName
import net.bytebuddy.matcher.ElementMatchers.named
import tech.httptoolkit.javaagent.advice.akka.OverrideHttpSettingsAdvice
import tech.httptoolkit.javaagent.advice.akka.ResetAllConnectionPoolsAdvice

// First, we hook outgoing connection creation, and ensure that new connections always go via the proxy & trust us:
class AkkaHttpTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(named("akka.http.scaladsl.HttpExt")) // Scala compiles Http()s methods here for some reason
            .transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(OverrideHttpSettingsAdvice::class.java)
                    .on(hasMethodName("_outgoingConnection"))
            )
    }
}

// Then, to ensure that any existing connections trust us too, we do a one-off connection pool reset,
// triggered by the new PoolMaster.dispatchRequest (seems to happen for every request). This seems to
// affects shared pools and individual pools too.
class AkkaPoolTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(named("akka.http.impl.engine.client.PoolMaster"))
            .transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(ResetAllConnectionPoolsAdvice::class.java)
                    .on(hasMethodName("dispatchRequest"))
            )
    }
}