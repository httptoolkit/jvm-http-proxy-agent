package tech.httptoolkit.javaagent.advice.jettyclient;

import net.bytebuddy.asm.Advice;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.net.ssl.SSLContext;

public class JettyReturnSslContextFactoryV10Advice {
    @Advice.OnMethodExit
    public static void getSslContextFactory(
            @Advice.Return(readOnly = false) SslContextFactory.Client returnValue
    ) throws Exception {
        SslContextFactory.Client sslFactory = new SslContextFactory.Client();
        sslFactory.setSslContext(SSLContext.getDefault());
        sslFactory.start();

        returnValue = sslFactory;
    }
}
