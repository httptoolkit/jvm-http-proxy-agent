package tech.httptoolkit.testapp.cases;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;

import java.util.concurrent.Future;

public class ApacheHttpAsyncClientV5Case extends ClientCase<CloseableHttpAsyncClient> {

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
        SimpleHttpRequest request = SimpleHttpRequests.get(url);
        Future<SimpleHttpResponse> future = client.execute(request, null);
        SimpleHttpResponse response = future.get();
        return response.getCode();
    }
}
