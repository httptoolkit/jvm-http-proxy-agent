package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.ssl.SSLContext;

public class OverrideSslContextFieldAdvice {

    @Advice.OnMethodEnter
    public static void beforeMethod(
            @Advice.FieldValue(value = "sslContext", readOnly = false) SSLContext sslContextField
    ) {
        sslContextField = HttpProxyAgent.getInterceptedSslContext();
    }

}
