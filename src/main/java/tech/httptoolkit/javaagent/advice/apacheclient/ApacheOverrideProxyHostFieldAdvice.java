package tech.httptoolkit.javaagent.advice.apacheclient;

import net.bytebuddy.asm.Advice;
import org.apache.commons.httpclient.ProxyHost;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class ApacheOverrideProxyHostFieldAdvice {

    @Advice.OnMethodExit
    public static void resetProxyHost(
        @Advice.FieldValue(value = "proxyHost", readOnly = false) ProxyHost proxyHostField
    ) {
        // After creating/changing HostConfiguration we override the proxy field:
        proxyHostField = new ProxyHost(
            HttpProxyAgent.getAgentProxyHost(),
            HttpProxyAgent.getAgentProxyPort()
        );
    }
}
