package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.utility.JavaModule
import tech.httptoolkit.javaagent.apacheclient.ApacheSetSslSocketFactoryAdvice
import tech.httptoolkit.javaagent.apacheclient.ApacheV4ReturnProxyRouteAdvice
import tech.httptoolkit.javaagent.apacheclient.ApacheV5ReturnProxyRouteAdvice

// Apache async client hooks depend on the non-async Apache client transformers, which successfully transform proxy
// configuration, but we need to separate re-transform TLS configuration too.

// To do so, we get all instances of TlsStrategy/SSLIOSessionStrategy, all of which seem to have an sslContext private
// field which they wrap around the connection their implementation upgrade(). Most of the real ones inherit from
// AbstractClientTlsStrategy, which does this, but there's other examples too. We hook upgrade(), so that that
// field is reset to use our context before any client TLS upgrade happens.

class ApacheClientTlsStrategyTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            // For v5 we need to hook into every client TlsStrategy (of which there are many).
            .type(
                hasSuperType(named("org.apache.hc.core5.http.nio.ssl.TlsStrategy"))
            ).and(
                // There are both Server & Client strategies with the same interface, and checking the name is the only
                // way to tell the difference:
                nameContains("Client")
            ).and(
                // All strategies either do this, or extend a class that does this (which will be intercepted
                // first by itself anyway)
                declaresField(named("sslContext"))
            ).and(
                not(isInterface())
            ).transform(this)
            // For v4, we do exactly the same, but there's only a single implementation:
            .type(
                named("org.apache.http.nio.conn.ssl.SSLIOSessionStrategy")
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        return builder
            .visit(
            Advice.to(OverrideSslContextFieldAdvice::class.java)
                .on(hasMethodName("upgrade"))
        );
    }
}
