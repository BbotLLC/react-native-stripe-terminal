package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;
import com.facebook.react.bridge.Promise;

/**
 * A [ReaderCallback] that notifies the [TerminalStateManager] that connection has completed
 */
public final class ConnectionCallback implements ReaderCallback {
    private final TerminalStateManager manager;
    private Promise promise;

    public ConnectionCallback(TerminalStateManager manager, Promise promise){
        super();
        this.manager = manager;
        this.promise = promise;
    }

    @Override
    public void onSuccess(Reader reader){
        this.manager.onConnectReader(reader, promise);
    }

    @Override
    public void onFailure(TerminalException e){
        this.promise.reject("ConnectionError", e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
