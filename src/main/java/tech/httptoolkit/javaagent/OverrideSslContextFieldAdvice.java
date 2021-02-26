package tech.httptoolkit.javaagent;

import net.bytebuddy.asm.Advice;

import javax.net.ssl.SSLContext;

public class OverrideSslContextFieldAdvice {

    @Advice.OnMethodEnter
    public static void beforeMethod(
            @Advice.FieldValue(value = "sslContext", readOnly = false) SSLContext sslContextField
    ) {
        sslContextField = HttpProxyAgent.getInterceptedSslContext();
    }

}
