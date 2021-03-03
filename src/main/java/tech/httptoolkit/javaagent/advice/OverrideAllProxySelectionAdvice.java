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

        boolean isHttp = scheme.equals("http") || scheme.equals("https");

        // We read from our custom variables, since we can't access HttpProxyAgent from a bootstrapped
        // class, and we use namespaced properties to make this extra reliable:
        String proxyHost = System.getProperty("tech.httptoolkit.proxyHost");
        int proxyPort = Integer.parseInt(System.getProperty("tech.httptoolkit.proxyPort"));

        boolean isRequestToProxy = uri.getHost().equals(proxyHost) && uri.getPort() == proxyPort;

        // For HTTP URIs going elsewhere, we override all proxy selection globally to go via our proxy:
        if (isHttp && !isRequestToProxy) {
            returnedProxies = Collections.singletonList(
                new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, proxyPort)
                )
            );
        }
    }
}
