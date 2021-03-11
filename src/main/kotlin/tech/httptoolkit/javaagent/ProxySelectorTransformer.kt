package tech.httptoolkit.javaagent

import net.bytebuddy.agent.builder.AgentBuilder
import net.bytebuddy.asm.Advice
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers.*
import tech.httptoolkit.javaagent.advice.OverrideAllProxySelectionAdvice
import tech.httptoolkit.javaagent.advice.OverrideUrlConnectionProxyAdvice
import tech.httptoolkit.javaagent.advice.SkipMethodAdvice

// To ensure that target applications don't override our ProxySelector (which we configure as the default), we
// also patch the ProxySelector class itself, to guarantee that our proxy is always always selected, and
// to stop anybody else changing the default.
class ProxySelectorTransformer(logger: TransformationLogger): MatchingAgentTransformer(logger) {
    override fun register(builder: AgentBuilder): AgentBuilder {
        return builder
            .type(
                hasSuperType(named("java.net.ProxySelector"))
            ).and(
                not(isInterface())
            ).transform(this)
    }

    override fun transform(builder: DynamicType.Builder<*>, loadAdvice: (String) -> Advice): DynamicType.Builder<*> {
        return builder
            // We patch *all* proxy selectors, so that even code which doesn't use the default
            // still returns our proxy regardless.
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.OverrideAllProxySelectionAdvice")
                .on(
                    hasMethodName<MethodDescription>("select")
                    .and(takesArguments(1))
                    .and(takesArgument(0, named("java.net.URI")))))
            // We already set the default ProxySelector on startup, before we intercept anything.
            // Here we patch ProxySelector so nobody can overwrite that later.
            .visit(loadAdvice("tech.httptoolkit.javaagent.advice.SkipMethodAdvice")
                .on(hasMethodName("setDefault")));
    }
}