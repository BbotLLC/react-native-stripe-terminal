package menu.bbot.reactnativestripeterminal;

import android.util.Log;
import com.stripe.stripeterminal.external.callable.TerminalListener;
import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.PaymentStatus;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.ReaderEvent;

class TerminalEventListener implements TerminalListener {

    private final RNStripeTerminalModule manager;

    public TerminalEventListener (RNStripeTerminalModule manager){
        super();
        this.manager = manager;
    }

    public void onUnexpectedReaderDisconnect(Reader reader) {
        manager.emit("UnexpectedDisconnect", true);
        Log.i("UnexpectedDisconnect", "Reader disconnected");
    }

    // Do I need to do this here and elsewhere?
    public void onConnectionStatusChange(ConnectionStatus status) {
        manager.emit("ConnectionStatusChange", status.toString());
        Log.i("TerminalEventListener.ConnectionStatusChange", status.toString());
    }

    public void onPaymentStatusChange(PaymentStatus status) {
        Log.i("PaymentStatusChange", status.toString());
        manager.emit("PaymentStatusChange", status.toString());
    }
}
