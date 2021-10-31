package menu.bbot.reactnativestripeterminal.callbacks;

import menu.bbot.reactnativestripeterminal.TerminalStateManager;
import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.models.TerminalException;

/**
 * A [Callback] that notifes the [TerminalStateManager] when discovery has completed
 */
public final class DiscoveryCallback implements Callback {

    private final TerminalStateManager manager;
    private final Promise promise;

    public DiscoveryCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    @Override
    public void onSuccess() {
        this.manager.onDiscoverReaders(promise);
    }

    @Override
    public void onFailure(TerminalException e) {
        this.promise.reject("DiscoveryError", e.getErrorMessage());
    }
}
