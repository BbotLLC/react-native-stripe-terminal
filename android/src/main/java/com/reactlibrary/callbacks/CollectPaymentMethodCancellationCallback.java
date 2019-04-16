package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;

/**
 * A [Callback] that notifies the [TerminalStateManager] that [Terminal.collectPaymentMethod] has
 * been canceled
 */
public final class CollectPaymentMethodCancellationCallback implements Callback {
    private final TerminalStateManager manager;

    public CollectPaymentMethodCancellationCallback(TerminalStateManager manager) {
        super();
        this.manager = manager;
    }

    public void onSuccess() {
        this.manager.onCancelCollectPaymentMethod();
    }

    public void onFailure(TerminalException e) {
        this.manager.onFailure(e);
    }
}