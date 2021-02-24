package tech.httptoolkit.javaagent.apacheclient;

import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.lang.reflect.Field;
import java.util.Arrays;

public class ApacheSetSslSocketFactoryAdvice {

    @Advice.OnMethodEnter
    public static void beforeCreateSocket(@Advice.This Object thisFactory) {
        // Before creating the socket - replace the SSL context so the new socket trusts us.

        boolean intercepted = false;
        for (String factoryFieldName : Arrays.asList("socketfactory", "socketFactory")) {
            try {
                // Detect which field(s) are present on this class
                Field field = thisFactory.getClass().getDeclaredField(factoryFieldName);

                // Allow ourselves to change the socket factory value
                field.setAccessible(true);

                // Overwrite the socket factory with our own:
                try {
                    field.set(thisFactory, HttpProxyAgent.getInterceptedSslContext().getSocketFactory());
                    intercepted = true;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Could not intercept Apache HttpClient HTTPS sockets");
                }
            } catch (NoSuchFieldException ignored) { }
        }

        if (!intercepted) {
            throw new IllegalStateException("Apache HttpClient interception setup failed");
        }
    }
}
