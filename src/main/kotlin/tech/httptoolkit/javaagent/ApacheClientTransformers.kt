package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import net.bytebuddy.utility.JavaModule

// For both v4 & v5 we override all implementations of the RoutePlanner interface, and we redefine all routes
// to go via our proxy instead of their existing configuration.

class ApacheClientRoutingV4Transformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder.type(
            hasSuperType(named("org.apache.http.conn.routing.HttpRoutePlanner"))
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
        return builder.visit(
            Advice.to(ApacheV4ReturnProxyRouteAdvice::class.java)
                .on(hasMethodName("determineRoute"))
        )
    }
}

class ApacheClientRoutingV5Transformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder.type(
            hasSuperType(named("org.apache.hc.client5.http.routing.HttpRoutePlanner"))
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
        return builder.visit(
            Advice.to(ApacheV5ReturnProxyRouteAdvice::class.java)
                .on(hasMethodName("determineRoute"))
        )
    }
}

// For certificates, we prepend to Apache SslConnectionSocketFactory's createLayeredSocket, so that before any
// socket is created, the SSL context is replaced with our configured SslSocketFactory that uses our configured
// SSLContext, which trusts our certificate, straight after initialization.

class ApacheSslSocketFactoryTransformer : MatchingAgentTransformer {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.apache.http.conn.ssl.SSLConnectionSocketFactory")
            ).transform(this)
            .type(
                named("org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory")
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
            Advice.to(ApacheSetSslSocketFactoryAdvice::class.java)
                .on(hasMethodName("createLayeredSocket"))
        );
    }
}
