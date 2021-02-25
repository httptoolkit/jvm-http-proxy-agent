package tech.httptoolkit.javaagent.advice.akka;

import net.bytebuddy.asm.Advice;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class ResetAllConnectionPoolsAdvice {

    public static AtomicBoolean connectionPoolsReset = new AtomicBoolean(false);

    @Advice.OnMethodEnter
    public static void resetConnectionsBeforeDispatch(
        @Advice.This Object thisPoolMaster
    ) throws Exception {
        boolean resetNeeded = ResetAllConnectionPoolsAdvice
                .connectionPoolsReset
                .compareAndSet(false, true);

        if (!resetNeeded) return;

        // Just once, when we first try to dispatch a request, reset all existing connection pools.
        Method shutdownMethod = thisPoolMaster.getClass().getDeclaredMethod("shutdownAll");
        shutdownMethod.invoke(thisPoolMaster);
    }
}
