package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;

/**
 * A [Callback] that notifies the [TerminalStateManager] when discovery has been canceled
 */
public final class DiscoveryCancellationCallback implements Callback {
    private final TerminalStateManager manager;

    public DiscoveryCancellationCallback(TerminalStateManager manager) {
        super();
        this.manager = manager;
    }

    public void onSuccess() {
        this.manager.onCancelDiscovery();
    }

    public void onFailure(TerminalException e) {
        this.manager.onFailure(e);
    }

}
