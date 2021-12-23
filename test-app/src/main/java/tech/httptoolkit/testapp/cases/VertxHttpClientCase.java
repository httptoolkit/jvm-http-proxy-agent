package tech.httptoolkit.testapp.cases;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class VertxHttpClientCase extends ClientCase<HttpClient> {
    @Override
    public HttpClient newClient(String url) throws Exception {
        return Vertx.vertx().createHttpClient();
    }

    @Override
    public int test(String url, HttpClient client) throws URISyntaxException, InterruptedException, ExecutionException {
        HttpClientResponse response = client
                .request(HttpMethod.GET, url)
                .compose(HttpClientRequest::send)
                .toCompletionStage().toCompletableFuture().get();
        return response.statusCode();
    }
}
