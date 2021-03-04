package tech.httptoolkit.javaagent.advice.ktor;

import io.ktor.network.tls.TLSConfig;
import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.ssl.X509TrustManager;

public class KtorResetTlsClientTrustAdvice {

    @Advice.OnMethodEnter
    public static void beforeOpenTLSSession(
        @Advice.Argument(value = 3, readOnly = false) TLSConfig tlsClientConfig
    ) throws Exception {
        // We're rewriting Kotlin bytecode from Java now, so some things get funky. Here it seems
        // that coroutines result in a double call where the outer call has no args, so we need this:
        if (tlsClientConfig == null) return;

        // Clone the config, but replace the trust manager with one that trusts only our certificate:
        tlsClientConfig = new TLSConfig(
                tlsClientConfig.getRandom(),
                tlsClientConfig.getCertificates(),
                (X509TrustManager) HttpProxyAgent
                        .getInterceptedTrustManagerFactory()
                        .getTrustManagers()[0],
                tlsClientConfig.getCipherSuites(),
                tlsClientConfig.getServerName()
        );
    }
}
