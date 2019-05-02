package com.reactlibrary.callbacks;

import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;

/**
 * A [Callback] that notifies the [TerminalStateManager] when discovery has been canceled
 */
public final class DiscoveryCancellationCallback implements Callback {
    private final TerminalStateManager manager;
    private Promise promise;

    public DiscoveryCancellationCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess() {
        this.manager.onCancelDiscovery(promise);
    }

    public void onFailure(TerminalException e) {
        promise.reject("DiscoveryCancellationError", e.getErrorMessage());
        this.manager.onFailure(e);
    }

}
