package tech.httptoolkit.javaagent.advice.akka;

import akka.http.impl.settings.HostConnectionPoolSetup;
import akka.http.scaladsl.ClientTransport;
import net.bytebuddy.asm.Advice;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// This is very similar to ResetOldPoolsAdvice, but applies to older Akka setups, which use many PoolGateway instances,
// one per config, rather than one PoolMaster instance. Otherwise the logic should be identical.
public class ResetOldGatewaysAdvice {

    public static Set<HostConnectionPoolSetup> resetPoolSetups = Collections.newSetFromMap(
        Collections.synchronizedMap(new WeakHashMap<>())
    );

    @Advice.OnMethodEnter
    public static void beforeDispatchRequest(
        @Advice.This Object thisPoolGateway,
        @Advice.FieldValue(value = "hcps") HostConnectionPoolSetup poolSetup
    ) throws Exception {
        // If a pool config has been changed to use our proxy already, then we're perfect
        ClientTransport transport = poolSetup.setup().settings().transport();
        boolean alreadyIntercepted = transport == ResetPoolSetupAdvice.interceptedProxyTransport;
        // If not, it's still OK, as long as we've previously reset the pool to ensure the connection was
        // re-established (we hook connection setup too, so all new conns are intercepted, even with old config)
        boolean alreadyReset = resetPoolSetups.contains(poolSetup);

        if (alreadyIntercepted || alreadyReset) return;

        // Otherwise this is a request to use a pre-existing connection pool which probably has connections open that
        // aren't using our proxy. We shutdown the pool before the request. It'll be restarted automatically when
        // the request does go through, but this ensures we re-establish connections (so it definitely gets intercepted)
        Method shutdownMethod = thisPoolGateway.getClass().getDeclaredMethod("shutdown");

        Future<?> shutdownFuture = (Future<?>) shutdownMethod.invoke(thisPoolGateway);

        // We wait a little, just to ensure the shutdown is definitely started before this request is dispatched.
        try {
            Await.result(shutdownFuture, Duration.apply(10, TimeUnit.MILLISECONDS));
        } catch (TimeoutException ignored) {}

        // Lastly, we remember this pool setup, so that we don't unnecessarily reset it again in future:
        resetPoolSetups.add(poolSetup);
    }

}
