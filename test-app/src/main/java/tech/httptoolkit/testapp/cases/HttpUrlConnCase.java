package tech.httptoolkit.testapp.cases;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUrlConnCase extends ClientCase<URL> {

    @Override
    public URL newClient(String url) throws MalformedURLException {
        return new URL(url);
    }

    @Override
    public int test(String url, URL urlInstance) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) urlInstance.openConnection();
        return connection.getResponseCode();
    }
}
