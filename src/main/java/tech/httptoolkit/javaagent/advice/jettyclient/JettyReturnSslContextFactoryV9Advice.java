package tech.httptoolkit.javaagent.advice.jettyclient;

import net.bytebuddy.asm.Advice;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import tech.httptoolkit.javaagent.HttpProxyAgent;

import javax.net.ssl.SSLContext;

public class JettyReturnSslContextFactoryV9Advice {

    @Advice.OnMethodExit
    public static void getSslContextFactory(
            @Advice.Return(readOnly = false) SslContextFactory returnValue
    ) throws Exception {
        SslContextFactory sslFactory = new JettyV9StubContextFactory();
        sslFactory.setSslContext(SSLContext.getDefault());
        try {
            sslFactory.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        returnValue = sslFactory;
    }
}
