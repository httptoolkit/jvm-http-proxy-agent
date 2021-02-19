package tech.httptoolkit.javaagent;

import net.bytebuddy.asm.Advice;

import javax.net.ssl.SSLSocketFactory;

public class ReturnSslSocketFactoryAdvice {
    @Advice.OnMethodExit
    public static void sslSocketFactory(@Advice.Return(readOnly = false) SSLSocketFactory returnValue) {
        returnValue = HttpProxyAgent.getInterceptedSslContext().getSocketFactory();
    }
}
