package tech.httptoolkit.testapp.cases;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class OkHttpV4Case extends ClientCase<OkHttpClient> {

    @Override
    public OkHttpClient newClient(String url) {
        return new OkHttpClient();
    }

    @Override
    public int test(String url, OkHttpClient client) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        response.body().close();
        return response.code();
    }
}
