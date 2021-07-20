package menu.bbot.reactnativestripeterminal;

import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.TerminalException;

/**
 * An `Activity` that should be notified when various [Terminal] actions have completed
 */
public interface TerminalStateManager {

    /**
     * Notify the `Activity` that collecting payment method has been canceled
     */
    public void onCancelCollectPaymentMethod(Promise promise);

    /**
     * Notify the `Activity` that collecting payment method has been canceled
     */
    public void onCancelReadReusableCard(Promise promise);

    /**
     * Notify the `Activity` that discovery has been canceled
     */
    public void onCancelDiscovery(Promise promise);

    /**
     * Notify the `Activity` that a payment method has been collected
     */
    public void onCollectPaymentMethod(PaymentIntent paymentIntent, Promise promise);

    /**
     * Notify the `Activity` that a [PaymentIntent] has been confirmed
     */
    public void onConfirmPaymentIntent(PaymentIntent paymentIntent, Promise promise);

    /**
     * Notify the `Activity` that a [Reader] has been connected to
     */
    public void onConnectReader(Reader reader, Promise promise);

    /**
     * Notify the `Activity` that a [PaymentIntent] has been created
     */
    public void onCreatePaymentIntent(PaymentIntent paymentIntent, Promise promise);

    /**
     * Notify the `Activity` that we've disconnected from all [Reader]s
     */
    public void onDisconnectReader(Promise promise);

    /**
     * Notify the `Activity` that [Reader] discovery has completed
     */
    public void onDiscoverReaders(Promise promise);

    /**
     * Notify the `Activity` that a [TerminalException] has been thrown
     */
    public void onFailure(TerminalException e);

}
