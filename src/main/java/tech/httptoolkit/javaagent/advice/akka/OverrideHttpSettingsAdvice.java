package tech.httptoolkit.javaagent.advice.akka;

import akka.http.scaladsl.ClientTransport;
import akka.http.scaladsl.ConnectionContext;
import akka.http.scaladsl.settings.ClientConnectionSettings;
import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.net.InetSocketAddress;

public class OverrideHttpSettingsAdvice {
    @Advice.OnMethodEnter
    public static void beforeOutgoingConnection(
        @Advice.Argument(value = 2, readOnly = false) ClientConnectionSettings clientSettings,
        @Advice.Argument(value = 3, readOnly = false) ConnectionContext connectionContext
    ) {
        // Change all new outgoing connections to use the proxy:
        clientSettings = clientSettings.withTransport(
            ClientTransport.httpsProxy(new InetSocketAddress(
                    HttpProxyAgent.getAgentProxyHost(),
                    HttpProxyAgent.getAgentProxyPort()
            ))
        );

        // Change all new outgoing connections to trust our certificate:
        connectionContext = ConnectionContext.httpsClient(HttpProxyAgent.getInterceptedSslContext());
    }
}
