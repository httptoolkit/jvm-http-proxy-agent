package tech.httptoolkit.javaagent.advice.asynchttpclient;

import net.bytebuddy.asm.Advice;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyServerSelector;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class AsyncHttpClientReturnProxySelectorAdvice {

    public static ProxyServerSelector proxyServerSelector = uri -> new ProxyServer.Builder(
        HttpProxyAgent.getAgentProxyHost(),
        HttpProxyAgent.getAgentProxyPort()
    ).build();

    @Advice.OnMethodExit
    public static void getProxyServerSelector(@Advice.Return(readOnly = false) ProxyServerSelector returnValue) {
        returnValue = proxyServerSelector;
    }
}
