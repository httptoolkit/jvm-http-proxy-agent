package tech.httptoolkit.testapp.cases;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import java.io.IOException;

public class RetrofitCase extends ClientCase<RetrofitCase.ExampleRetrofitClient> {

    @Override
    public RetrofitCase.ExampleRetrofitClient newClient(String url) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .build();
        return retrofit.create(RetrofitCase.ExampleRetrofitClient.class);
    }

    @Override
    public int test(String url, RetrofitCase.ExampleRetrofitClient client) throws IOException {
        Response<Void> response = client.exampleRequest().execute();
        return response.code();
    }

    public interface ExampleRetrofitClient {
        @GET("/")
        Call<Void> exampleRequest();
    }
}