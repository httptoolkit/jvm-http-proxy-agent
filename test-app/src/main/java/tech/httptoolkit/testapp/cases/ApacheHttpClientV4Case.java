package tech.httptoolkit.testapp.cases;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class ApacheHttpClientV4Case extends ClientCase<HttpClient> {

    @Override
    public HttpClient newClient(String url) {
        return HttpClients.createDefault();
    }

    @Override
    public int test(String url, HttpClient client) throws IOException {
        HttpGet request = new HttpGet(url);
        return client.execute(request, response -> response.getStatusLine().getStatusCode());
    }
}
