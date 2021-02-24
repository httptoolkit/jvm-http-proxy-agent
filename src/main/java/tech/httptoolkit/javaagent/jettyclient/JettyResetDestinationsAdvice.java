package tech.httptoolkit.javaagent.jettyclient;

import net.bytebuddy.asm.Advice;
import org.eclipse.jetty.client.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class JettyResetDestinationsAdvice {

    // Track each client with a weak ref, to avoid unnecessary reflection overhead by only
    // initializing them once, instead of every request
    public static Set<Object> patchedHttpClients = Collections.newSetFromMap(new WeakHashMap());

    @Advice.OnMethodEnter
    public static void beforeResolveDestination(
        @Advice.This Object thisHttpClient
        // ^ Note that we can't use the real HttpClient type here, since this class is redefining it, so it would
        // cause a circular reference that breaks patching completely.
    ) {
        if (patchedHttpClients.contains(thisHttpClient)) return;

        // If this is the first time that we've seen this client, it's possible that it existed before we attached,
        // and it might have some existing open connections that don't use our proxy. To fix that, just once per
        // client, we use reflection to get the destinations (cached connections) and reset them.
        try {
            @SuppressWarnings("unchecked")
            Map<Origin, HttpDestination> destinations = (Map<Origin, HttpDestination>)
                    thisHttpClient.getClass().getDeclaredField("destinations").get(thisHttpClient);

            // Reset this destinations list:
            for (HttpDestination destination : destinations.values()) {
                destination.close();
            }
            destinations.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        patchedHttpClients.add(thisHttpClient);
    }
}
