package tech.httptoolkit.javaagent.advice.vertxclient;

import io.vertx.core.net.ProxyOptions;
import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class VertxHttpClientReturnProxyConfigurationAdvice {

    @Advice.OnMethodExit
    public static void getProxyConfiguration(@Advice.Return(readOnly = false) ProxyOptions returnValue) {
        returnValue = new ProxyOptions().setHost(HttpProxyAgent.getAgentProxyHost()).setPort(HttpProxyAgent.getAgentProxyPort());
    }
}
