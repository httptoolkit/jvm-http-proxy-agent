package tech.httptoolkit.testapp.cases;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

public class OkHttpV2Case extends ClientCase<OkHttpClient> {

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
        return response.code();
    }
}
