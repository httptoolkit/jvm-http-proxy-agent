package tech.httptoolkit.testapp.cases;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.engines.ClientHttpEngineBuilder43;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

public class RestEasyWithApacheHttpClientV4Case extends ClientCase<ResteasyClient> {

    @Override
    public ResteasyClient newClient(String url) throws MalformedURLException {
        ResteasyClientBuilderImpl resteasyClientBuilder = new ResteasyClientBuilderImpl();
        resteasyClientBuilder.sslContext(getSslContext());
        resteasyClientBuilder.httpEngine(new ClientHttpEngineBuilder43()
                .resteasyClientBuilder(resteasyClientBuilder).build());
        return resteasyClientBuilder.build();
    }

    @Override
    public int test(String url, ResteasyClient resteasyClient) throws IOException {
        return resteasyClient.target(URI.create(url)).request().get().getStatus();
    }

    private SSLContext getSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
