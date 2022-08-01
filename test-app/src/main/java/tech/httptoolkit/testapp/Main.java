package tech.httptoolkit.testapp;

import tech.httptoolkit.testapp.cases.*;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.util.Map.entry;

public class Main {

    private static final Map<String, ClientCase<?>> cases = Map.ofEntries(
        entry("apache-v3", new ApacheHttpClientV3Case()),
        entry("apache-v4", new ApacheHttpClientV4Case()),
        entry("rest-easy-with-apache-v4", new RestEasyWithApacheHttpClientV4Case()),
        entry("apache-v5", new ApacheHttpClientV5Case()),
        entry("apache-async-v4", new ApacheHttpAsyncClientV4Case()),
        entry("apache-async-v5", new ApacheHttpAsyncClientV5Case()),
        entry("http-url-conn", new HttpUrlConnCase()),
        entry("java-http-client", new JavaHttpClientCase()),
        entry("okhttp-v2", new OkHttpV2Case()),
        entry("okhttp-v4", new OkHttpV4Case()),
        entry("retrofit", new RetrofitCase()),
        entry("jetty-client", new JettyClientCase()),
        entry("async-http-client", new AsyncHttpClientCase()),
        entry("spring-web", new SpringWebClientCase()),
        entry("ktor-cio", new KtorCioCase()),
        entry("akka-req-http", new AkkaRequestClientCase()),
        entry("akka-host-http", new AkkaHostClientCase()),
        entry("vertx-httpclient", new VertxHttpClientCase()),
        entry("vertx-webclient", new VertxWebClientCase())
    );

    public static void main(String[] args) throws Exception {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String pid = runtimeName.split("@")[0];
        System.out.println("PID: " + pid); // Purely for convenient manual attachment to this process

        String url = "https://httpbin.org/404/"; // Always returns a 404, quelle surprise
        ExecutorService executor = Executors.newCachedThreadPool();

        while (true) {
            AtomicBoolean allSuccessful = new AtomicBoolean(true);

            List<Future<Void>> tests = executor.invokeAll(cases.entrySet().stream().map((entry) -> ((Callable<Void>) () -> {
                String name = entry.getKey();
                ClientCase<?> clientCase = entry.getValue();

                try {
                    int result = clientCase.testNew(url);
                    if (result != 200) {
                        System.out.println("Unexpected result for new " + name + ": " + result);
                        allSuccessful.set(false);
                    }
                } catch (Throwable e) {
                    System.out.println("Unexpected failure for new " + name + ": " + e.toString());
                    System.out.println(e.toString());
                    allSuccessful.set(false);
                }

                try {
                    int result = clientCase.testExisting(url);
                    if (result != 200) {
                        System.out.println("Unexpected result for existing " + name + ": " + result);
                        allSuccessful.set(false);
                    }
                } catch (Throwable e) {
                    System.out.println("Unexpected failure for existing " + name + ": " + e.toString());
                    allSuccessful.set(false);
                }

                return null;
            })).collect(Collectors.toList()));

            // Wait for all tests to complete
            for (Future<Void> f: tests) { f.get(); }

            if (allSuccessful.get()) {
                System.out.println("All cases intercepted successfully");
                System.exit(0);
            }

            //noinspection BusyWait
            sleep(1000);
        }

    }

}
