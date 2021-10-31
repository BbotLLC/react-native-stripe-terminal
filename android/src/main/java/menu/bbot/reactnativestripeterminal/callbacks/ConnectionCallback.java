package menu.bbot.reactnativestripeterminal.callbacks;
import menu.bbot.reactnativestripeterminal.TerminalStateManager;
import com.facebook.react.bridge.Promise;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.TerminalException;

import org.jetbrains.annotations.NotNull;

/**
 * A [ReaderCallback] that notifies the [TerminalStateManager] that connection has completed
 */
public final class ConnectionCallback implements ReaderCallback {
    private final TerminalStateManager manager;
    private Promise promise;

    public ConnectionCallback(TerminalStateManager manager, Promise promise){
        super();
        this.manager = manager;
        this.promise = promise;
    }

    @Override
    public void onSuccess(@NotNull Reader reader){

        this.manager.onConnectReader(reader, promise);

    }

    @Override
    public void onFailure(TerminalException e){
        this.promise.reject("ConnectionError", e.getErrorMessage());
        this.manager.onFailure(e);
    }
}
