package tech.httptoolkit.javaagent.advice.ktor;

import io.ktor.network.tls.TLSConfig;
import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class KtorResetProxyFieldAdvice {

    @Advice.OnMethodEnter
    public static void beforeExecute(
        @Advice.FieldValue(value = "proxy", readOnly = false) Proxy proxyField
    ) throws Exception {
        proxyField = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
            HttpProxyAgent.getAgentProxyHost(),
            HttpProxyAgent.getAgentProxyPort()
        ));
    }
}
