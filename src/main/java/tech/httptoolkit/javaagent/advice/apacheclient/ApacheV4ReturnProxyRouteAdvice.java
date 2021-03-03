package tech.httptoolkit.javaagent.advice.apacheclient;

import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import java.net.*;

public class ApacheV4ReturnProxyRouteAdvice {
    @Advice.OnMethodExit
    public static void determineRoute(
        // We type this dynamically, because in some cases (notably Gradle) we seemingly can't reach the
        // HttpRoute type from ByteBuddy, only at runtime.
        @Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returnValue
    ) {
        HttpRoute existingValue = (HttpRoute) returnValue;
        // We guarantee that the default proxy selector is always our own. This ensures that we can
        // always grab the proxy URL without needing to access our injected classes.
        Proxy proxy = ProxySelector.getDefault().select(URI.create("https://example.com")).get(0);
        InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();

        returnValue = new HttpRoute(
            existingValue.getTargetHost(),
            existingValue.getLocalAddress(),
            new HttpHost(proxyAddress.getHostString(), proxyAddress.getPort()),
            existingValue.isSecure()
        );
    }
}
