package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;

public class OverrideUrlConnectionProxyAdvice {

    @Advice.OnMethodEnter
    public static void openConnection(
        @Advice.FieldValue(value = "protocol") String urlProtocol,
        @Advice.Argument(value = 0, readOnly = false) Proxy proxyArgument
    ) {
        if (urlProtocol.equals("http") || urlProtocol.equals("https")) {
            // We can't access HttpProxyAgent here or even thisd class, since we're in the bootstrap loader, but
            // we've already stored a proxy on ProxySelector for all URLs, so we can just use that directly:
            proxyArgument = ProxySelector.getDefault().select(
                    URI.create("http://example.com")
            ).get(0);
        }
    }
}
