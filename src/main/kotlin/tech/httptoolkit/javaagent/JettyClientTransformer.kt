package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import org.eclipse.jetty.util.ssl.SslContextFactory
import tech.httptoolkit.javaagent.advice.jettyclient.JettyResetDestinationsAdvice
import tech.httptoolkit.javaagent.advice.jettyclient.JettyReturnProxyConfigurationAdvice
import tech.httptoolkit.javaagent.advice.jettyclient.JettyReturnSslContextFactoryV10Advice
import tech.httptoolkit.javaagent.advice.jettyclient.JettyReturnSslContextFactoryV9Advice

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
class JettyClientTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {

    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("org.eclipse.jetty.client.HttpClient")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.jettyclient.JettyReturnProxyConfigurationAdvice")
                .on(hasMethodName("getProxyConfiguration")))
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.jettyclient.JettyReturnSslContextFactoryV10Advice")
                .on(hasMethodName<MethodDescription>("getSslContextFactory").and(
                    returns(SslContextFactory.Client::class.java)
                )))
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.jettyclient.JettyReturnSslContextFactoryV9Advice")
                .on(hasMethodName<MethodDescription>("getSslContextFactory").and(
                    returns(SslContextFactory::class.java)
                )))
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.jettyclient.JettyResetDestinationsAdvice")
                .on(hasMethodName("resolveDestination")))
    }
}