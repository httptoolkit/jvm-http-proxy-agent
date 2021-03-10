package tech.httptoolkit.testapp.cases;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.HostConnectionPool;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Uri;
import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.util.Try;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// Here we use a host-based persistent Akka connection pool, just to make sure that that case is covered too, in
// addition to the request client case.
public class AkkaHostClientCase extends ClientCase<
    Flow<Pair<HttpRequest, NotUsed>, Pair<Try<HttpResponse>, NotUsed>, HostConnectionPool>
> {

    private static final ActorSystem system = ExtendedActorSystem.create();

    @Override
    public Flow<Pair<HttpRequest, NotUsed>, Pair<Try<HttpResponse>, NotUsed>, HostConnectionPool> newClient(String url) throws Exception {
        Uri uri = Uri.create(url);
        return new Http((ExtendedActorSystem) system)
            .cachedHostConnectionPoolHttps(ConnectHttp.toHost(uri)); // HTTPS required here, or HTTP is *always* used
    }

    @Override
    public int test(
            String url,
            Flow<Pair<HttpRequest, NotUsed>, Pair<Try<HttpResponse>, NotUsed>, HostConnectionPool> clientFlow
    ) throws URISyntaxException, ExecutionException, InterruptedException {
        Source<Pair<HttpRequest, NotUsed>, NotUsed> requestSource = Source.single(
            new Pair<>(HttpRequest.create(url), null)
        );

        CompletableFuture<Pair<Try<HttpResponse>, NotUsed>> responseFuture = requestSource
            .via(clientFlow)
            .runWith(Sink.head(), system)
            .toCompletableFuture();

        HttpResponse response = responseFuture.get().first().get();
        response.discardEntityBytes(system);
        return response.status().intValue();
    }
}
