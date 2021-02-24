package tech.httptoolkit.testapp.cases;


import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class JettyClientCase extends ClientCase<HttpClient> {

    @Override
    public HttpClient newClient(String url) throws Exception {
        HttpClient client = new HttpClient();
        client.start();
        return client;
    }

    @Override
    public void stopClient(HttpClient client) throws Exception {
        client.stop();
    }

    @Override
    public int test(String url, HttpClient client) throws InterruptedException, ExecutionException, TimeoutException {
        ContentResponse res = client.GET(url);
        return res.getStatus();
    }
}
