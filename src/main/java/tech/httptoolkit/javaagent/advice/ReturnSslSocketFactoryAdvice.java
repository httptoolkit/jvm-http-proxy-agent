package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.ssl.SSLSocketFactory;

public class ReturnSslSocketFactoryAdvice {
    @Advice.OnMethodExit
    public static void sslSocketFactory(@Advice.Return(readOnly = false) SSLSocketFactory returnValue) {
        returnValue = HttpProxyAgent.getInterceptedSslContext().getSocketFactory();
    }
}
