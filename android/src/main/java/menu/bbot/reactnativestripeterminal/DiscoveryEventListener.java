package menu.bbot.reactnativestripeterminal;

import android.util.Log;

import com.stripe.stripeterminal.DiscoveryListener;
import com.stripe.stripeterminal.Reader;

import java.util.List;

public class DiscoveryEventListener implements DiscoveryListener {

    private final RNStripeTerminalModule module;

    public DiscoveryEventListener(RNStripeTerminalModule module){
        this.module = module;
    }

    public void onUpdateDiscoveredReaders(List<Reader> readers){
        Log.d("Available Readers: ", readers.toString());
        module.setAvailableReaders(readers);

    }

}