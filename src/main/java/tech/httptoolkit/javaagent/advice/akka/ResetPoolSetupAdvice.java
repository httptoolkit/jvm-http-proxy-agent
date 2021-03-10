package tech.httptoolkit.javaagent.advice.akka;

import akka.http.scaladsl.ClientTransport;
import akka.http.scaladsl.ConnectionContext;
import akka.http.scaladsl.settings.ConnectionPoolSettings;
import net.bytebuddy.asm.Advice;
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
        @Advice.FieldValue(value = "settings", readOnly = false) ConnectionPoolSettings settings,
        @Advice.FieldValue(value = "connectionContext", readOnly = false) ConnectionContext connContext
    ) {
        // Change all new outgoing connections to use the proxy:
        settings = settings.withTransport(interceptedProxyTransport);

        // Change all new outgoing connections to trust our certificate:
        connContext = ConnectionContext.httpsClient(HttpProxyAgent.getInterceptedSslContext());
    }
}
