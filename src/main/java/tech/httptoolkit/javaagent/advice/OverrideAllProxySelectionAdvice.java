package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class OverrideAllProxySelectionAdvice {

    @Advice.OnMethodExit
    public static void selectProxy(
            @Advice.Argument(value = 0) URI uri,
            @Advice.Return(readOnly = false) List<Proxy> returnedProxies
    ) {
        String scheme = uri.getScheme();

        // For HTTP URIs only, we override all proxy selection globally to select our proxy instead:
        if (scheme.equals("http") || scheme.equals("https")) {
            returnedProxies = Collections.singletonList(
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
                    // We read from our custom variables, since we can't access HttpProxyAgent from a bootstrapped
                    // class, and we use namespaced properties to make this extra reliable:
                    System.getProperty("tech.httptoolkit.proxyHost"),
                    Integer.parseInt(System.getProperty("tech.httptoolkit.proxyPort"))
                ))
            );
        }
    }
}
