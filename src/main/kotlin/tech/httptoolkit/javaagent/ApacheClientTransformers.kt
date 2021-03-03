package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.apacheclient.*

// For both v4 & v5 we override all implementations of the RoutePlanner interface, and we redefine all routes
// to go via our proxy instead of their existing configuration.

class ApacheClientRoutingV4Transformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder.type(
            hasSuperType(named("org.apache.http.conn.routing.HttpRoutePlanner"))
        ).and(
            not(isInterface())
        ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder.visit(
            Advice.to(ApacheV4ReturnProxyRouteAdvice::class.java)
                .on(hasMethodName("determineRoute"))
        )
    }
}

class ApacheClientRoutingV5Transformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder.type(
            hasSuperType(named("org.apache.hc.client5.http.routing.HttpRoutePlanner"))
        ).and(
            not(isInterface())
        ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder.visit(
            Advice.to(ApacheV5ReturnProxyRouteAdvice::class.java)
                .on(hasMethodName("determineRoute"))
        )
    }
}

// For certificates, we prepend to Apache SslConnectionSocketFactory's createLayeredSocket, so that before any
// socket is created, the SSL context is replaced with our configured SslSocketFactory that uses our configured
// SSLContext, which trusts our certificate, straight after initialization.

class ApacheSslSocketFactoryTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.apache.http.conn.ssl.SSLConnectionSocketFactory")
            ).transform(this)
            .type(
                named("org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
            Advice.to(ApacheSetSslSocketFactoryAdvice::class.java)
                .on(hasMethodName("createLayeredSocket"))
        );
    }
}

// Meanwhile, for V3 we need to do something totally different: we patch HostConfiguration to apply a proxy to
// all new configurations (and ignore changes), we patch HttpMethodDirector to update existing configurations
// as they're used, and we patch Protocol to change the SslSocketFactory on all secure protocols.

class ApacheHostConfigurationTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.apache.commons.httpclient.HostConfiguration")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            // Override the proxy field value for all new configurations, and for any attempts to call
            // setProxy/ProxyHost. We don't no-op these, because we want to call them ourselves later on
            // existing configs to reset them - we don't just want to ignore this.
            .visit(
                Advice.to(ApacheOverrideProxyHostFieldAdvice::class.java)
                    .on(isConstructor<MethodDescription>()
                        .or(hasMethodName("setProxy"))
                        .or(hasMethodName("setProxyHost"))
                    )
            )
    }
}

// Whenever an HttpMethodDirector is used, we reset the proxy in the passed configuration. This uses the above
// hooks, which ensure that setProxyHost(anything) automatically loads & sets our intercepted proxy.
// We *don't* want to reset all proxy hosts in all existing configurations, because that's a) quite tricky and
// b) some are used as keys in existing direct connections in pools, and we don't want to match those later.
class ApacheHttpMethodDirectorTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.apache.commons.httpclient.HttpMethodDirector")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(ApacheSetConfigProxyHostAdvice::class.java)
                    .on(hasMethodName("executeMethod"))
            )
    }
}

// Every v3 configuration has a protocol, and each one can build sockets in its own unique way. Here, we patch
// all of them so that all _secure_ protocols trust only our certificate, and nothing else. This would
// be an issue for a generic TCP client, but for HTTPS we know we should be the only authority present.
class ApacheProtocolTransformer(logger: TransformationLogger) : MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.apache.commons.httpclient.protocol.Protocol")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>): DynamicType.Builder<*> {
        return builder
            .visit(
                Advice.to(ApacheReturnCustomSslProtocolSocketFactoryAdvice::class.java)
                    .on(hasMethodName("getSocketFactory"))
            )
    }
}