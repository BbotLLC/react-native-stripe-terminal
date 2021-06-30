package menu.bbot.reactnativestripeterminal.callbacks;

import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.callable.Callback;
import com.stripe.stripeterminal.model.external.TerminalException;

import menu.bbot.reactnativestripeterminal.TerminalStateManager;


/**
 * A [Callback] that notifies the [TerminalStateManager] that [Terminal.collectPaymentMethod] has
 * been canceled
 */
public final class ReadReusableCardCancellationCallback implements Callback {
    private final TerminalStateManager manager;
    private Promise promise;

    public ReadReusableCardCancellationCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess() {
        this.manager.onCancelReadReusableCard(this.promise);
    }

    public void onFailure(TerminalException e) {
        promise.reject("ReadReusableCardError", e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
