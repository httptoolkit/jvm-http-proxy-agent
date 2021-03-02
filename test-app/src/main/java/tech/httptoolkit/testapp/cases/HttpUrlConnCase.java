package tech.httptoolkit.testapp.cases;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

// We take over default HttpUrlConnections with no problem by setting config vars, but that doesn't apply in all
// cases, e.g. if the target code manages its own proxy config, or disables it. We intercept here to forcibly
// ensure that our proxy is _always_ used, regardless of the passed proxy configuration.
public class HttpUrlConnCase extends ClientCase<URL> {

    @Override
    public URL newClient(String url) throws MalformedURLException {
        return new URL(url);
    }

    @Override
    public int test(String url, URL urlInstance) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) urlInstance.openConnection(Proxy.NO_PROXY);
        return connection.getResponseCode();
    }
}
