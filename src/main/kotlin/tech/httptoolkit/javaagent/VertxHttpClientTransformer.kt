package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*

// Ensures that the proxy is used by overriding the getProxyOptions method of HttpClientImpl
// to always return our proxy information
class VertxHttpClientTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                named("io.vertx.core.http.impl.HttpClientImpl")
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.vertxclient.VertxHttpClientReturnProxyConfigurationAdvice")
                .on(hasMethodName("getProxyOptions")))
    }
}
