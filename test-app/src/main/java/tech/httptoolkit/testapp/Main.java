package tech.httptoolkit.testapp;

import tech.httptoolkit.testapp.cases.*;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class Main {

    private static final Map<String, ClientCase<?>> cases = Map.of(
            "apache-v4", new ApacheHttpClientV4Case(),
            "apache-v5", new ApacheHttpClientV5Case(),
            "http-url-conn", new HttpUrlConnCase(),
            "java-http-client", new JavaHttpClientCase(),
            "okhttp-v2", new OkHttpV2Case(),
            "okhttp-v4", new OkHttpV4Case(),
            "retrofit", new RetrofitCase()
    );

    public static void main(String[] args) throws Exception {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        String pid = runtimeName.split("@")[0];
        System.out.println("PID: " + pid); // Purely for convenient manual attachment to this process

        String url = "https://example.test"; // Invalid URL: this should always fail to resolve

        while (true) {
            AtomicBoolean allSuccessful = new AtomicBoolean(true);

            cases.forEach((name, clientCase) -> {
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
            });

            cases.forEach((name, clientCase) -> {
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
            });

            if (allSuccessful.get()) {
                System.out.println("All cases intercepted successfully");
                System.exit(0);
            }

            //noinspection BusyWait
            sleep(1000);
        }

    }

}
