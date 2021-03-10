package tech.httptoolkit.javaagent.advice.akka;

import akka.http.scaladsl.ClientTransport;
import akka.http.scaladsl.settings.ConnectionPoolSettings;
import akka.http.javadsl.ConnectionContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.net.InetSocketAddress;

public class ResetPoolSetupAdvice {

    // We use this to avoid re-instantiating the proxy endlessly, but also to recognize intercepted
    // and pre-existing settings configurations when they're used.
    public static ClientTransport interceptedProxyTransport = ClientTransport.httpsProxy(
        new InetSocketAddress(
            HttpProxyAgent.getAgentProxyHost(),
            HttpProxyAgent.getAgentProxyPort()
        )
    );

    @Advice.OnMethodExit
    public static void afterConstructor(
        @Advice.FieldValue(value = "settings", readOnly = false, typing = Assigner.Typing.DYNAMIC) ConnectionPoolSettings settings,
        @Advice.FieldValue(value = "connectionContext", readOnly = false, typing = Assigner.Typing.DYNAMIC) ConnectionContext connContext
    ) {
        // Change all new outgoing connections to use the proxy:
        settings = settings.withTransport(interceptedProxyTransport);

        // Change all new outgoing connections to trust our certificate:
        if (connContext.isSecure()) {
            connContext = OverrideHttpSettingsAdvice.interceptedConnectionContext;
        }
    }
}
