package tech.httptoolkit.testapp.cases;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

public class SpringWebClientCase extends ClientCase<WebClient> {
    @Override
    public WebClient newClient(String url) throws Exception {
        return WebClient.create();
    }

    @Override
    public int test(String url, WebClient client) throws URISyntaxException {
        Mono<Integer> result = client.get()
                .uri(new URI(url))
                .accept(MediaType.ALL)
                .exchangeToMono(response -> Mono.just(response.rawStatusCode()));

        //noinspection ConstantConditions
        return result.block();
    }
}
