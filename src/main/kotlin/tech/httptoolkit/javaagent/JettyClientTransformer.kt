package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.utility.JavaModule
import net.bytebuddy.matcher.ElementMatchers.*
import java.util.*

/**
 * Transforms the JettyClient to use our proxy & trust our certificate.
 *
 * For new clients, we just need to override the proxyConfiguration and
 * sslContextFactory properties on the HTTP client itself.
 *
 * For existing clients, we do that, and we also reset the destinations
 * (internal connection pools) when resolveDestination is first called
 * on each client.
 */
class JettyClientTransformer : MatchingAgentTransformer {

    companion object {
        // This is used to cache the clients that have been fully configured (had any existing destinations
        // reset) so that we don't unnecessarily reset them again later.
        @JvmStatic
        public val patchedHttpClients: MutableSet<Any> = Collections.newSetFromMap(WeakHashMap())
    }

    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.eclipse.jetty.client.HttpClient")
            ).transform(this)
    }

    override fun transform(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classLoader: ClassLoader?,
        module: JavaModule?
    ): DynamicType.Builder<*>? {
        return builder
            .visit(Advice.to(JettyReturnProxyConfigurationAdvice::class.java)
                .on(hasMethodName("getProxyConfiguration")))
            .visit(Advice.to(JettyReturnSslContextFactoryAdvice::class.java)
                .on(hasMethodName("getSslContextFactory")))
            .visit(Advice.to(JettyResetDestinationsAdvice::class.java)
                .on(hasMethodName("resolveDestination")))
    }
}