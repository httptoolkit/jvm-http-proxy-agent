package tech.httptoolkit.testapp.cases;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.IOException;

public class ApacheHttpClientV5Case extends ClientCase<HttpClient> {

    @Override
    public HttpClient newClient(String url) {
        return HttpClients.createDefault();
    }

    @Override
    public int test(String url, HttpClient client) throws IOException {
        HttpGet request = new HttpGet(url);
        return client.execute(request, response -> response.getCode());
    }
}
