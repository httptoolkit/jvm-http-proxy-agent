package tech.httptoolkit.javaagent.advice.apacheclient;

import net.bytebuddy.asm.Advice;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class ApacheV4ReturnProxyRouteAdvice {
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
