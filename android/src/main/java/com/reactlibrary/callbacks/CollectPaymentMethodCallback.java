package com.reactlibrary.callbacks;

import com.stripe.stripeterminal.*;
import com.reactlibrary.TerminalStateManager;
import com.facebook.react.bridge.Promise;

/**
 * A [PaymentIntentCallback] that notifies the [TerminalStateManager] that payment method collection
 * has completed
 */
public final class CollectPaymentMethodCallback implements PaymentIntentCallback {
    private final TerminalStateManager manager;
    private Promise promise;

    public CollectPaymentMethodCallback(TerminalStateManager manager, Promise promise) {
        super();
        this.manager = manager;
        this.promise = promise;
    }

    public void onSuccess(PaymentIntent paymentIntent) {
        this.manager.onCollectPaymentMethod(paymentIntent, promise);
    }

    public void onFailure(TerminalException e) {
        promise.reject(e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
