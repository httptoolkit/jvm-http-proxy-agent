package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.ReturnProxyAddressAdvice
import tech.httptoolkit.javaagent.advice.reactornetty.ReactorNettyResetAllConfigAdvice
import tech.httptoolkit.javaagent.advice.reactornetty.ReactorNettyResetHttpClientSecureSslAdvice
import tech.httptoolkit.javaagent.advice.reactornetty.ReactorNettyV09ResetProxyProviderFieldAdvice

// To patch Reactor-Netty's v1 HTTP client, we hook the constructor of the client itself. It has a constructor
// that receives the config as part of every single HTTP request - we hook that to reset the relevant
// config props every time they're used.

private val matchConfigConstructor = isConstructor<MethodDescription>()
    .and(takesArguments(1))
    .and(takesArgument(0,
        named("reactor.netty.http.client.HttpClientConfig")
    ))

class ReactorNettyClientConfigTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {

    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                hasSuperType(named("reactor.netty.http.client.HttpClient"))
            ).and(
                not(isInterface())
            ).and(
                // This matches v1+ only, where the config is passed into the constructor repeatedly, and can
                // be mutated there. v0.9 is handled separately below.
                declaresMethod(matchConfigConstructor)
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReactorNettyResetAllConfigAdvice::class.java)
                .on(matchConfigConstructor)
            )
    }
}

// In v0.9, that wasn't the case. Instead, the SSL provider and proxy provider are passed as arguments to
// and stored within various client classes. Here, we patch all their constructors to reset those fields
// immediately after instantiation, ensuring our values replace the given arguments.

// First, the sslProvider field:
class ReactorNettyHttpClientSecureTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("reactor.netty.http.client.HttpClientSecure")
            ).and(
                declaresField(named("sslProvider"))
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReactorNettyResetHttpClientSecureSslAdvice::class.java)
                .on(isConstructor()))
    }
}

// Then each of the important cases where a proxy provider is stored:
class ReactorNettyProxyProviderTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                declaresField(named<FieldDescription>("proxyProvider").and(
                    // This only applies to v0.9+ which uses this package name, not v1+ where ProxyProvider
                    // lives in reactor.netty.transport (handled by the other transformer above)
                    fieldType(named("reactor.netty.tcp.ProxyProvider")))
                )
            ).and(
                named<TypeDescription>(
                    "reactor.netty.http.client.HttpClientConnect\$MonoHttpConnect"
                ).or(
                    named(
                        "reactor.netty.http.client.HttpClientConnect\$HttpClientHandler"
                    )
                )
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReactorNettyV09ResetProxyProviderFieldAdvice::class.java)
                .on(isConstructor()))
    }
}

// Then, on top of all that, we also forcibly set the socket address for all outgoing HTTP connections, because that's
// that's the goal, and the above proxyProvider logic doesn't properly cover everything as proxy logic is spread across
// a few places including the generic TCP clients (which we shouldn't touch). This is a bit messy/risky, but only
// applies to v0.9, since v1+ stores the config in a properly structured way.
class ReactorNettyOverrideRequestAddressTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                declaresField(named<FieldDescription>("proxyProvider").and(
                    // This ensures this only applies to v0.9+ which uses this package name, not v1+ where
                    // ProxyProvider lives in reactor.netty.transport (handled separately above).
                    fieldType(named("reactor.netty.tcp.ProxyProvider")))
                )
            ).and(
                named(
                    "reactor.netty.http.client.HttpClientConnect\$HttpClientHandler"
                )
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(Advice.to(ReturnProxyAddressAdvice::class.java)
                .on(hasMethodName("get")))
    }
}