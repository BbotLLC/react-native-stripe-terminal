package menu.bbot.reactnativestripeterminal;

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONObject;

public class TokenProvider implements ConnectionTokenProvider {

    private String url;
    private String authToken;

    public TokenProvider(String url, String authToken){
        this.url = url;
        this.authToken = authToken;
    }


    @Override
    public void fetchConnectionToken(ConnectionTokenCallback callback) {
        try {
            // Your backend should call v1/terminal/connection_tokens and return the
            // JSON response from Stripe. When the request to your backend succeeds,
            // return the `secret` from the response to the SDK.

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .header("Authorization", this.authToken)
                    .url(this.url)
                    .build();

            Response response = client.newCall(request).execute();

            String jsonResponse = response.body().string();
            JSONObject obj = new JSONObject(jsonResponse);

            callback.onSuccess(obj.getString("secret"));
        } catch (Exception e) {
            callback.onFailure(
                new ConnectionTokenException("Failed to fetch connection token", e)
            );
        }
    }
}
