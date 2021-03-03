package tech.httptoolkit.javaagent.advice.apacheclient;

import net.bytebuddy.asm.Advice;
import org.apache.commons.httpclient.HostConfiguration;

public class ApacheSetConfigProxyHostAdvice {

    @Advice.OnMethodEnter
    public static void beforeMakingRequests(
        @Advice.FieldValue(value = "hostConfiguration") HostConfiguration hostConfiguration
    ) {
        // Elsewhere, we hook setProxyHost to reset the proxy to our configured version whenever it's called.
        // Then, here we hook various methods to call it before they use the config:
        hostConfiguration.setProxyHost(null); // null here is ignored as this method is already hooked
    }
}
