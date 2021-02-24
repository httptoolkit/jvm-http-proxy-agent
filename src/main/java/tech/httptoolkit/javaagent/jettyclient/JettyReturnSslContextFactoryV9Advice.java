package tech.httptoolkit.javaagent.jettyclient;

import net.bytebuddy.asm.Advice;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class JettyReturnSslContextFactoryV9Advice {

    @Advice.OnMethodExit
    public static void getSslContextFactory(@Advice.Return(readOnly = false) SslContextFactory returnValue) {
        SslContextFactory sslFactory = new JettyV9StubContextFactory();
        sslFactory.setSslContext(HttpProxyAgent.getInterceptedSslContext());
        try {
            sslFactory.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        returnValue = sslFactory;
    }
}
