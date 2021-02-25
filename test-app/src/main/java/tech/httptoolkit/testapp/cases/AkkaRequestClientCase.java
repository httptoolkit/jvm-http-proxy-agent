package tech.httptoolkit.testapp.cases;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;

import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AkkaRequestClientCase extends ClientCase<Http> {

    private static final ActorSystem system = ActorSystem.create();

    @Override
    public Http newClient(String url) throws Exception {
        return Http.get(system);
    }

    @Override
    public int test(String url, Http client) throws URISyntaxException, ExecutionException, InterruptedException {
        CompletableFuture<HttpResponse> responseFuture = client
                .singleRequest(HttpRequest.create(url))
                .toCompletableFuture();
        HttpResponse response = responseFuture.get();
        return response.status().intValue();
    }
}
