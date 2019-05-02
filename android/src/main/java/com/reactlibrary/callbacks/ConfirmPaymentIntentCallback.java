package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;
import com.facebook.react.bridge.Promise;

/**
 * A [PaymentIntentCallback] that notifies the [TerminalStateManager] that [PaymentIntent]
 * confirmation has completed
 */
public final class ConfirmPaymentIntentCallback implements PaymentIntentCallback {
    private final TerminalStateManager manager;
    private Promise promise;

    public ConfirmPaymentIntentCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess(PaymentIntent paymentIntent) {
        this.manager.onConfirmPaymentIntent(paymentIntent, promise);
    }

    public void onFailure(TerminalException e) {
        promise.reject("ConfirmPaymentIntentError", e.getErrorMessage());
        this.manager.onFailure(e);
    }


}