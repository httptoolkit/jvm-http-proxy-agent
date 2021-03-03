package tech.httptoolkit.testapp.cases;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;

public class ApacheHttpClientV3Case extends ClientCase<HttpClient> {

    @Override
    public HttpClient newClient(String url) {
        return new HttpClient();
    }

    @Override
    public int test(String url, HttpClient client) throws IOException {
        HttpMethod method = new GetMethod(url);
        client.executeMethod(method);
        method.releaseConnection();
        return method.getStatusCode();
    }
}
