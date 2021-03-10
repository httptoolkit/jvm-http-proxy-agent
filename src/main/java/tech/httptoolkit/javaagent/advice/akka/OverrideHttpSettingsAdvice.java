package tech.httptoolkit.javaagent.advice.akka;

import akka.http.javadsl.ClientTransport;
import akka.http.javadsl.ConnectionContext;
import akka.http.javadsl.settings.ClientConnectionSettings;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import java.net.InetSocketAddress;
import java.util.Arrays;

public class OverrideHttpSettingsAdvice {

    public static final boolean hasHttpsSettingsMethod =
            Arrays.stream(ConnectionContext.class.getDeclaredMethods())
                    .anyMatch(method -> method.getName().equals("httpsClient"));

    public static final ConnectionContext interceptedConnectionContext = hasHttpsSettingsMethod
            // For 10.2+:
            ? ConnectionContext.httpsClient(HttpProxyAgent.getInterceptedSslContext())
            // For everything before then:
            : ConnectionContext.https(HttpProxyAgent.getInterceptedSslContext());

    @Advice.OnMethodEnter
    public static void beforeOutgoingConnection(
        @Advice.Argument(value = 2, readOnly = false, typing = Assigner.Typing.DYNAMIC) ClientConnectionSettings clientSettings,
        @Advice.Argument(value = 3, readOnly = false, typing = Assigner.Typing.DYNAMIC) ConnectionContext connectionContext
    ) {
        // Change all new outgoing connections to use the proxy:
        clientSettings = clientSettings.withTransport(
            ClientTransport.httpsProxy(new InetSocketAddress(
                HttpProxyAgent.getAgentProxyHost(),
                HttpProxyAgent.getAgentProxyPort()
            ))
        );

        // Change all new outgoing connections to trust our certificate:
        if (connectionContext.isSecure()) {
            connectionContext = OverrideHttpSettingsAdvice.interceptedConnectionContext;
        }
    }
}
