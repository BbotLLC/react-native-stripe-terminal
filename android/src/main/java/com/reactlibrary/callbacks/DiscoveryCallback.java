package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;
import com.facebook.react.bridge.Promise;
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

    public void onSuccess() {
        this.manager.onDiscoverReaders(promise);

    }

    public void onFailure(TerminalException e) {
        this.promise.reject(e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
