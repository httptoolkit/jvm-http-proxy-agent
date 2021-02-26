package tech.httptoolkit.testapp.cases;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;

import java.util.concurrent.Future;

public class ApacheHttpAsyncClientV4Case extends ClientCase<CloseableHttpAsyncClient> {

    @Override
    public CloseableHttpAsyncClient newClient(String url) {
        CloseableHttpAsyncClient client = HttpAsyncClients.createDefault();
        client.start();
        return client;
    }

    @Override
    public void stopClient(CloseableHttpAsyncClient client) throws Exception {
        client.close();
    }

    @Override
    public int test(String url, CloseableHttpAsyncClient client) throws Exception {
        HttpGet request = new HttpGet(url);
        Future<HttpResponse> future = client.execute(request, null);
        HttpResponse response = future.get();
        return response.getStatusLine().getStatusCode();
    }
}
