package tech.httptoolkit.testapp.cases;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class JavaHttpClientCase extends ClientCase<HttpClient> {

    @Override
    public HttpClient newClient(String url) {
        return HttpClient.newHttpClient();
    }

    @Override
    public int test(String url, HttpClient client) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(url))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }
}
