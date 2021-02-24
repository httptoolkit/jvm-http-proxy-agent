package tech.httptoolkit.javaagent.asynchttpclient;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.ssl.SSLException;

public class AsyncHttpClientReturnSslContextAdvice {
    @Advice.OnMethodExit
    public static void getSslContext(@Advice.Return(readOnly = false) SslContext returnValue) {
        try {
            returnValue = SslContextBuilder
                    .forClient()
                    .trustManager(HttpProxyAgent.getInterceptedTrustManagerFactory())
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }
}
