package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;

public class ReturnSslContextAdvice {
    @Advice.OnMethodExit
    public static void sslContext(@Advice.Return(readOnly = false) SSLContext returnValue) {
        try {
            returnValue = SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
