package menu.bbot.reactnativestripeterminal.callbacks;

import menu.bbot.reactnativestripeterminal.TerminalStateManager;
import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.TerminalException;

/**
 * A [PaymentIntentCallback] that notifies the [TerminalStateManager] that [PaymentIntent] creation
 * has completed
 */
public final class CreatePaymentIntentCallback implements PaymentIntentCallback {
    private final TerminalStateManager manager;
    private Promise promise;

    public CreatePaymentIntentCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess(PaymentIntent paymentIntent) {
        this.manager.onCreatePaymentIntent(paymentIntent, promise);
    }

    public void onFailure(TerminalException e) {
        this.promise.reject("CreatePaymentIntentError", e.getErrorMessage());
        this.manager.onFailure(e);
    }

}
