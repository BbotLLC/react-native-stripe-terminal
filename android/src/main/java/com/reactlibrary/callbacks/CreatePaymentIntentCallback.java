package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;
import com.facebook.react.bridge.Promise;

/**
 * A [PaymentIntentCallback] that notifies the [TerminalStateManager] that [PaymentIntent] creation
 * has completed
 */
public final class CreatePaymentIntentCallback implements PaymentIntentCallback {
    private final TerminalStateManager manager;
    private Promise promise;

    public CreatePaymentIntentCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess(PaymentIntent paymentIntent) {
        this.manager.onCreatePaymentIntent(paymentIntent, promise);
    }

    public void onFailure(TerminalException e) {
        this.promise.reject(e.getErrorMessage());
        this.manager.onFailure(e);
    }

}