package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;
import com.facebook.react.bridge.Promise;

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