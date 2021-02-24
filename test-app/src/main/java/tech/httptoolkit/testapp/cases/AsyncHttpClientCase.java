package tech.httptoolkit.testapp.cases;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.asynchttpclient.Dsl.*;

public class AsyncHttpClientCase extends ClientCase<AsyncHttpClient> {

    @Override
    public AsyncHttpClient newClient(String url) {
        return asyncHttpClient();
    }

    @Override
    public int test(String url, AsyncHttpClient client) throws ExecutionException, InterruptedException {
        Request request = get(url).build();
        Future<Response> whenResponse = client.executeRequest(request);
        return whenResponse.get().getStatusCode();
    }
}
