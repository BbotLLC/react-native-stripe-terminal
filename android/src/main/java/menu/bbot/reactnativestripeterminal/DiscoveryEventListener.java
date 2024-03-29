package menu.bbot.reactnativestripeterminal;

import android.util.Log;

import com.stripe.stripeterminal.external.callable.DiscoveryListener;
import com.stripe.stripeterminal.external.models.Reader;

import java.util.List;

public class DiscoveryEventListener implements DiscoveryListener {

    private final RNStripeTerminalModule manager;

    public DiscoveryEventListener(RNStripeTerminalModule module){
        this.manager = module;
    }

    @Override
    public void onUpdateDiscoveredReaders(List<Reader> readers) {
        Log.i("RNStripeTerminal", "Available Readers: "+readers.toString());

        manager.setAvailableReaders(readers);
    }


}
