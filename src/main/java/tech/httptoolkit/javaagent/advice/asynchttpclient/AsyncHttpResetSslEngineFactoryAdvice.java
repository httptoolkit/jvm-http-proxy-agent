package tech.httptoolkit.javaagent.advice.asynchttpclient;

import net.bytebuddy.asm.Advice;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.SslEngineFactory;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class AsyncHttpResetSslEngineFactoryAdvice {

    // Track each ChannelManager with a weak ref, to avoid unnecessary reflection overhead by only
    // initializing them once, instead of every request
    public static Set<Object> patchedChannelManagers = Collections.newSetFromMap(
        Collections.synchronizedMap(new WeakHashMap<>())
    );

    @Advice.OnMethodEnter
    public static void createSslHandler(
        @Advice.This Object thisChannelManager
    ) {
        if (patchedChannelManagers.contains(thisChannelManager)) return;

        try {
            Class<?> ChannelManager = thisChannelManager.getClass();

            SslEngineFactory sslEngineFactory = (SslEngineFactory) ChannelManager
                    .getDeclaredField("sslEngineFactory")
                    .get(thisChannelManager);

            AsyncHttpClientConfig config = (AsyncHttpClientConfig) ChannelManager
                    .getDeclaredField("config")
                    .get(thisChannelManager);

            // Reinitialize the SSL Engine from the config (which uses our new cert)
            // before building the SSL handler.
            sslEngineFactory.init(config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        patchedChannelManagers.add(thisChannelManager);
    }
}
