package tech.httptoolkit.javaagent.advice.reactornetty;

import io.netty.handler.ssl.SslContextBuilder;
import net.bytebuddy.asm.Advice;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.tcp.SslProvider;
import reactor.netty.transport.ClientTransportConfig;
import reactor.netty.transport.ProxyProvider;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;

public class ReactorNettyResetAllConfigAdvice {

    public static final ProxyProvider agentProxyProvider = ProxyProvider.builder()
        .type(ProxyProvider.Proxy.HTTP)
        .address(new InetSocketAddress(
            HttpProxyAgent.getAgentProxyHost(),
            HttpProxyAgent.getAgentProxyPort()
        ))
        .build();

    public static final SslProvider agentSslProvider;

    public static final Field configSslField;
    public static final Field proxyProviderField;

    static {
        try {
            // Initialize our intercepted SSL provider:
            agentSslProvider = SslProvider.builder()
                .sslContext(
                    SslContextBuilder
                    .forClient()
                    .trustManager(HttpProxyAgent.getInterceptedTrustManagerFactory())
                    .build()
                ).build();

            // Rewrite the fields we want to mess with in the client config:
            configSslField = HttpClientConfig.class.getDeclaredField("sslProvider");
            configSslField.setAccessible(true);

            proxyProviderField = ClientTransportConfig.class.getDeclaredField("proxyProvider");
            proxyProviderField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Netty's HTTP Client works by creating a new client subclass, and passing the existing client's
    // config. We hook that here: we rewrite the config whenever it's used, affecting all clients
    // involved, and in practice anybody using config anywhere.

    @Advice.OnMethodEnter
    public static void beforeConstructor(
        @Advice.Argument(value=0) HttpClientConfig baseHttpConfig
    ) {
        try {
            configSslField.set(baseHttpConfig, agentSslProvider);
            proxyProviderField.set(baseHttpConfig, agentProxyProvider);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}