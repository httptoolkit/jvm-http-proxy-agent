package tech.httptoolkit.javaagent.reactornetty;

import io.netty.handler.ssl.SslContextBuilder;
import net.bytebuddy.asm.Advice;
import reactor.netty.tcp.SslProvider;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class ReactorNettyResetHttpClientSecureSslAdvice {

    public static final SslProvider agentSslProvider;

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // In v0.9 versions of Reactor Netty, the sslProvider is stored on HttpClientSecure. Here we hook that class's
    // constructor and replace the SSL provider as soon as it's set.

    @Advice.OnMethodExit
    public static void afterConstructor(
        @Advice.FieldValue(value = "sslProvider", readOnly = false) SslProvider sslProviderField
    ) {
        sslProviderField = agentSslProvider;
    }
}