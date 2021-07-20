package menu.bbot.reactnativestripeterminal.callbacks;

import menu.bbot.reactnativestripeterminal.TerminalStateManager;
import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.models.TerminalException;

/**
 * A [Callback] that notifies the [TerminalStateManager] when disconnect has completed
 */
public final class DisconnectCallback implements Callback {
    private final TerminalStateManager manager;
    private Promise promise;

    public DisconnectCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess() {
        this.manager.onDisconnectReader(promise);
    }

    public void onFailure(TerminalException e) {
        if(promise != null)
            promise.reject("DisconnectError", e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
