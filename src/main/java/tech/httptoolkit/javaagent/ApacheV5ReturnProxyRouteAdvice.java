package tech.httptoolkit.javaagent;

import net.bytebuddy.asm.Advice;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.client5.http.HttpRoute;

public class ApacheV5ReturnProxyRouteAdvice {
    @Advice.OnMethodExit
    public static void determineRoute(
            @Advice.Return(readOnly = false) HttpRoute returnValue
    ) {
        returnValue = new HttpRoute(
                returnValue.getTargetHost(),
                returnValue.getLocalAddress(),
                new HttpHost(
                    HttpProxyAgent.getAgentProxyHost(),
                    HttpProxyAgent.getAgentProxyPort()
                ),
                returnValue.isSecure()
        );
    }
}
