package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;
import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

public class OverrideSslContextFieldAdvice {

    @Advice.OnMethodEnter
    public static void beforeMethod(
            @Advice.FieldValue(value = "sslContext", readOnly = false) SSLContext sslContextField
    ) throws NoSuchAlgorithmException {
        sslContextField = SSLContext.getDefault();
    }

}
