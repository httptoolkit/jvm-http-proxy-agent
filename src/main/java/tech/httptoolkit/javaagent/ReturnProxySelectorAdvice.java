package tech.httptoolkit.javaagent;

import net.bytebuddy.asm.Advice;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.Optional;

public class ReturnProxySelectorAdvice {
    @Advice.OnMethodExit
    public static void proxy(@Advice.Return(readOnly = false) Optional<ProxySelector> returnValue) {
        returnValue = Optional.of(ProxySelector.getDefault());
    }
}