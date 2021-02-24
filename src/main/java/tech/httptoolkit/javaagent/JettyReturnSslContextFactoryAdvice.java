package tech.httptoolkit.javaagent;

import net.bytebuddy.asm.Advice;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class JettyReturnSslContextFactoryAdvice {
    @Advice.OnMethodExit
    public static void getSslContextFactory(@Advice.Return(readOnly = false) SslContextFactory.Client returnValue) {
        SslContextFactory.Client sslFactory = new SslContextFactory.Client();
        sslFactory.setSslContext(HttpProxyAgent.getInterceptedSslContext());
        try {
            sslFactory.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        returnValue = sslFactory;
    }
}
