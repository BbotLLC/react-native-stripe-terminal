package menu.bbot.reactnativestripeterminal;

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;

import org.jetbrains.annotations.NotNull;
import android.util.Log;

public class TokenProvider implements ConnectionTokenProvider {

    RNStripeTerminalModule manager;
    ConnectionTokenCallback callback;

    public TokenProvider(RNStripeTerminalModule manager){
        this.manager = manager;
    }


    @Override
    public void fetchConnectionToken(@NotNull ConnectionTokenCallback callback) {
        this.callback = callback;
        this.manager.emit("RequestConnectionToken", true);
    }
}
