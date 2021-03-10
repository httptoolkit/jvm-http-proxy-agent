package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.akka.*

// First, we hook outgoing connection creation, and ensure that new connections always go via the proxy & trust us:
class AkkaHttpTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(named("akka.http.scaladsl.HttpExt")) // Scala compiles Http()s methods as 'Ext' for some reason
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

// Second, when a connection pool setup is created (part of creating any connection pool, but also for
// sending any individual request) we change its configuration. This isn't strictly necessary given the above,
// but helps generally, and makes the 3rd step possible.
class AkkaPoolSettingsTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(named("akka.http.impl.settings.ConnectionPoolSetup"))
            .transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(ResetPoolSetupAdvice::class.java)
                    .on(isConstructor())
            )
    }
}

// Then, to ensure that any existing connections trust us too, we monitor all calls to dispatchRequest, and reset
// any pools that don't have intercepted configuration (so preare -existing), just once per pool id.
class AkkaPoolTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(named("akka.http.impl.engine.client.PoolMaster"))
            .transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(ResetOldPoolsAdvice::class.java)
                    .on(hasMethodName("dispatchRequest"))
            )
    }
}

// The above works perfectly for new Akka, but as a last step we duplicate the 3rd step for slightly older versions:
class AkkaGatewayTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(named("akka.http.impl.engine.client.PoolGateway")) // Exists on <10.2.0
            .transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(ResetOldGatewaysAdvice::class.java)
                    .on(hasMethodName("apply"))
            )
    }
}
