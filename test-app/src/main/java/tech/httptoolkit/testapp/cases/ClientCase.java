package tech.httptoolkit.testapp.cases;

public abstract class ClientCase<T> {

    public abstract T newClient(String url) throws Exception;
    public abstract int test(String url, T client) throws Exception;

    private String existingClientUrl;
    private T client;

    public T existingClient(String url) throws Exception {
        if (client == null) {
            client = newClient(url);
            existingClientUrl = url;
        } else if (!existingClientUrl.equals(url)) {
            throw new RuntimeException("Existing client must get the same URL every time");
        }

        return client;
    }

    public void stopClient(T client) throws Exception {
        // Do nothing by default, but subclasses can override this
    }

    public int testNew(String url) throws Exception {
        T client = newClient(url);
        int result = test(url, client);
        stopClient(client);
        return result;
    }

    public int testExisting(String url) throws Exception {
        return test(url, existingClient(url));
    }

}
