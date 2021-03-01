package tech.httptoolkit.javaagent.reactornetty;

import net.bytebuddy.asm.Advice;
import reactor.netty.tcp.ProxyProvider;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.net.InetSocketAddress;


// Reset the proxyProvider field to use our own intercepted proxy after certain constructors
// complete. Note that this uses the v0.9 proxyProvider class, so it would fail if applied to
// when v1 is loaded in the target app.
public class ReactorNettyV09ResetProxyProviderFieldAdvice {

    public static final ProxyProvider agentProxyProvider = ProxyProvider.builder()
            .type(ProxyProvider.Proxy.HTTP)
            .address(new InetSocketAddress(
                    HttpProxyAgent.getAgentProxyHost(),
                    HttpProxyAgent.getAgentProxyPort()
            ))
            .build();

    @Advice.OnMethodExit
    public static void afterConstructor(
        @Advice.FieldValue(value = "proxyProvider", readOnly = false) ProxyProvider proxyProviderField
    ) {
        proxyProviderField = agentProxyProvider;
    }
}