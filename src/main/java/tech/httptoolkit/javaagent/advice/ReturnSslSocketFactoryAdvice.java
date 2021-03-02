package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.NoSuchAlgorithmException;

public class ReturnSslSocketFactoryAdvice {
    @Advice.OnMethodExit
    public static void sslSocketFactory(@Advice.Return(readOnly = false) SSLSocketFactory returnValue) {
        try {
            returnValue = SSLContext.getDefault().getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
