package tech.httptoolkit.testapp.cases;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

public class VertxWebClientCase extends ClientCase<WebClient> {
    @Override
    public WebClient newClient(String url) throws Exception {
        return WebClient.create(Vertx.vertx());
    }

    @Override
    public int test(String url, WebClient client) throws URISyntaxException, InterruptedException, ExecutionException {
        HttpResponse<Buffer> response = client
                .request(HttpMethod.GET, url)
                .send()
                .toCompletionStage().toCompletableFuture().get();
        return response.statusCode();
    }
}
