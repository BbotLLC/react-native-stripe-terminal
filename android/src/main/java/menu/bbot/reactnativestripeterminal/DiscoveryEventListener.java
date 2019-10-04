package menu.bbot.reactnativestripeterminal;

import android.util.Log;

import com.stripe.stripeterminal.callable.DiscoveryListener;
import com.stripe.stripeterminal.model.external.Reader;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DiscoveryEventListener implements DiscoveryListener {

    private final RNStripeTerminalModule module;

    public DiscoveryEventListener(RNStripeTerminalModule module){
        this.module = module;
    }

    @Override
    public void onUpdateDiscoveredReaders(@NotNull List<? extends Reader> readers) {
        Log.d("Available Readers: ", readers.toString());
        module.setAvailableReaders(readers);
    }


}