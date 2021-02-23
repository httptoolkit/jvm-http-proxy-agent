package tech.httptoolkit.testapp.cases;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public abstract class ClientCase<T> {

    public abstract T newClient(String url) throws MalformedURLException;
    public abstract int test(String url, T client) throws IOException, InterruptedException;

    private String existingClientUrl;
    private T client;

    public T existingClient(String url) throws MalformedURLException {
        if (client == null) {
            client = newClient(url);
            existingClientUrl = url;
        } else if (!existingClientUrl.equals(url)) {
            throw new RuntimeException("Existing client must get the same URL every time");
        }

        return client;
    }

    public int testNew(String url) throws IOException, InterruptedException {
        return test(url, newClient(url));
    }

    public int testExisting(String url) throws IOException, InterruptedException {
        return test(url, existingClient(url));
    }

}
