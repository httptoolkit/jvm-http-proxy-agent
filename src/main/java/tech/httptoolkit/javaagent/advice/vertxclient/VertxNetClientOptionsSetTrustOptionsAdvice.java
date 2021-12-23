package tech.httptoolkit.javaagent.advice.vertxclient;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.TrustOptions;
import net.bytebuddy.asm.Advice;
import tech.httptoolkit.javaagent.HttpProxyAgent;

public class VertxNetClientOptionsSetTrustOptionsAdvice {

    @Advice.OnMethodExit
    public static void afterConstructor(
            @Advice.This NetClientOptions thisNetClientOptions,
            @Advice.Argument(value = 0) ClientOptionsBase other
    ) {
        if (other instanceof HttpClientOptions) {
            thisNetClientOptions.setTrustOptions(TrustOptions.wrap(HttpProxyAgent.getInterceptedTrustManagerFactory().getTrustManagers()[0]));
        }
    }
}
