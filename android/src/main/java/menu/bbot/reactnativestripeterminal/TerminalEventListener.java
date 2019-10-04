package menu.bbot.reactnativestripeterminal;

import android.util.Log;
import com.stripe.stripeterminal.*;
import com.stripe.stripeterminal.callable.TerminalListener;
import com.stripe.stripeterminal.model.external.*;

class TerminalEventListener implements TerminalListener {

    private final RNStripeTerminalModule manager;

    public TerminalEventListener (RNStripeTerminalModule manager){
        super();
        this.manager = manager;
    }

    public void onReportReaderEvent(ReaderEvent event) {
        Log.i("ReaderEvent", event.toString());

        manager.emit("ReaderEvent", event.toString());
    }

    public void onReportLowBatteryWarning() {
        manager.emit("LowBatteryWarning", null);
        Log.i("LowBatteryWarning", "");
    }

    public void onUnexpectedReaderDisconnect(Reader reader) {
        manager.emit("UnexpectedDisconnect", true);
        Log.i("UnexpectedDisconnect", "Reader disconnected");
    }

    public void onConnectionStatusChange(ConnectionStatus status) {
        manager.emit("ConnectionStatusChange", status.toString());
        Log.i("ConnectionStatusChange", status.toString());
    }

    public void onPaymentStatusChange(PaymentStatus status) {
        Log.i("PaymentStatusChange", status.toString());
        manager.emit("PaymentStatusChange", status.toString());
    }
}