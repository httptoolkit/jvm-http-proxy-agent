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

    // Netty's HTTP Client works by creating a new client subclass, and passing the existing client's
    // config. We hook that here: we rewrite the config whenever it's used, affecting all clients
    // involved, and in practice anybody using config anywhere.

    @Advice.OnMethodEnter
    public static void beforeConstructor(
        @Advice.Argument(value=0) HttpClientConfig baseHttpConfig
    ) throws Exception {
        // It would be nice to do this setup statically, but due to how some classloader configs work (e.g. Spring) it
        // seems this can fail when run in a static block, so we just repeat the process for every interception:
        final SslProvider agentSslProvider = SslProvider.builder()
            .sslContext(
                    SslContextBuilder
                            .forClient()
                            .trustManager(HttpProxyAgent.getInterceptedTrustManagerFactory())
                            .build()
            ).build();

        final ProxyProvider agentProxyProvider = ProxyProvider.builder()
            .type(ProxyProvider.Proxy.HTTP)
            .address(new InetSocketAddress(
                    HttpProxyAgent.getAgentProxyHost(),
                    HttpProxyAgent.getAgentProxyPort()
            ))
            .build();

        Field configSslField;
        Field proxyProviderField;

        try {
            // Rewrite the fields we want to mess with in the client config:
            configSslField = HttpClientConfig.class.getDeclaredField("sslProvider");
            configSslField.setAccessible(true);

            proxyProviderField = ClientTransportConfig.class.getDeclaredField("proxyProvider");
            proxyProviderField.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        configSslField.set(baseHttpConfig, agentSslProvider);
        proxyProviderField.set(baseHttpConfig, agentProxyProvider);
    }
}