package tech.httptoolkit.javaagent.advice;

import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ReturnProxyAddressAdvice {
    @Advice.OnMethodExit
    public static void proxy(@Advice.Return(readOnly = false) SocketAddress returnValue) {
        returnValue = new InetSocketAddress(
                HttpProxyAgent.getAgentProxyHost(),
                HttpProxyAgent.getAgentProxyPort()
        );
    }
}
