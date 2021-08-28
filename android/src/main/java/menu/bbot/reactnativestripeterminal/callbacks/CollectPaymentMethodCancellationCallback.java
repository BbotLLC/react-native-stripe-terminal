package menu.bbot.reactnativestripeterminal.callbacks;

import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.external.api.ApiError;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.models.TerminalException;

import menu.bbot.reactnativestripeterminal.TerminalStateManager;


/**
 * A [Callback] that notifies the [TerminalStateManager] that [Terminal.collectPaymentMethod] has
 * been canceled
 */
public final class CollectPaymentMethodCancellationCallback implements Callback {
    private final TerminalStateManager manager;
    private Promise promise;

    public CollectPaymentMethodCancellationCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    @Override
    public void onSuccess() {
        this.manager.onCancelCollectPaymentMethod(this.promise);
    }

    @Override
    public void onFailure(TerminalException e) {
        promise.reject("CollectPaymentMethodError", e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
