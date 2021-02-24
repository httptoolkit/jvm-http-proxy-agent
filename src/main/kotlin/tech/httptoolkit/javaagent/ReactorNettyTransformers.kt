package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.utility.JavaModule
import reactor.netty.http.client.HttpClientConfig
import tech.httptoolkit.javaagent.reactornetty.ReactorNettyResetAllConfigAdvice

// To patch Reactor-Netty's HTTP client, we hook the constructor of the client itself. It has a constructor
// that receives the config as part of every single HTTP request - we hook that to reset the relevant
// config props every time they're used.

class ReactorNettyClientConfigTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                hasSuperType(named("reactor.netty.http.client.HttpClient"))
            ).and(
                not(isInterface())
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReactorNettyResetAllConfigAdvice::class.java)
                .on(isConstructor<MethodDescription>().and(
                    takesArguments(HttpClientConfig::class.java)
                )))
    }
}
