package tech.httptoolkit.javaagent.advice.apacheclient;

import net.bytebuddy.asm.Advice;

import javax.net.ssl.SSLContext;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ApacheSetSslSocketFactoryAdvice {

    @Advice.OnMethodEnter
    public static void beforeCreateSocket(@Advice.This Object thisFactory) throws Exception {
        // Before creating the socket - replace the SSL context so the new socket trusts us.

        boolean intercepted = false;
        for (String factoryFieldName : Arrays.asList("socketfactory", "socketFactory")) {
            try {
                // Detect which field(s) are present on this class
                Field field = thisFactory.getClass().getDeclaredField(factoryFieldName);

                // Allow ourselves to change the socket factory value
                field.setAccessible(true);

                // Overwrite the socket factory with our own:
                field.set(thisFactory, SSLContext.getDefault().getSocketFactory());
                intercepted = true;
            } catch (NoSuchFieldException ignored) { }
        }

        if (!intercepted) {
            throw new IllegalStateException("Apache HttpClient interception setup failed");
        }
    }
}
