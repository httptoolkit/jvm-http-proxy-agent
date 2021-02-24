package tech.httptoolkit.javaagent.jettyclient;

import net.bytebuddy.asm.Advice;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.ProxyConfiguration;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class JettyReturnProxyConfigurationAdvice {
    @Advice.OnMethodExit
    public static void getProxyConfiguration(@Advice.Return(readOnly = false) ProxyConfiguration returnValue) {
        Origin.Address proxyAddress = new Origin.Address(HttpProxyAgent.getAgentProxyHost(), HttpProxyAgent.getAgentProxyPort());

        ProxyConfiguration proxyConfig = new ProxyConfiguration();
        proxyConfig.getProxies().add(new HttpProxy(proxyAddress, false));

        returnValue = proxyConfig;
    }
}
