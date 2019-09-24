package menu.bbot.reactnativestripeterminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.stripe.stripeterminal.*;

import menu.bbot.reactnativestripeterminal.callbacks.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import android.os.Build;
import android.util.Log;

import menu.bbot.reactnativestripeterminal.callbacks.CollectPaymentMethodCallback;
import menu.bbot.reactnativestripeterminal.callbacks.CollectPaymentMethodCancellationCallback;
import menu.bbot.reactnativestripeterminal.callbacks.ConfirmPaymentIntentCallback;
import menu.bbot.reactnativestripeterminal.callbacks.ConnectionCallback;
import menu.bbot.reactnativestripeterminal.callbacks.CreatePaymentIntentCallback;
import menu.bbot.reactnativestripeterminal.callbacks.DisconnectCallback;
import menu.bbot.reactnativestripeterminal.callbacks.DiscoveryCallback;
import menu.bbot.reactnativestripeterminal.callbacks.DiscoveryCancellationCallback;

public class RNStripeTerminalModule
        extends ReactContextBaseJavaModule
        implements TerminalStateManager, ReaderDisplayListener {

    private ReactApplicationContext reactContext;

    private List<Reader> availableReaders;

    private Cancelable cancelableDiscovery;
    private Cancelable cancelableCollect;
    private Cancelable cancelableUpdate;
    private Cancelable cancelableInstall;

    private Boolean isDiscovering;

    public ReaderSoftwareUpdate availableUpdate;

    private PaymentIntent currentPaymentIntent;

    public RNStripeTerminalModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;
    }


    @Override
    public String getName() {
        return "StripeTerminal";
    }

    // Optional method to return constant values
    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        //constants.put(DURATION_SHORT_KEY, );
        //constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
        return constants;
    }

    @ReactMethod
    public void isInitialized(Promise promise) {
        promise.resolve(_isInitialized());
    }

    private boolean _isInitialized(){
        try {
            Terminal t = Terminal.getInstance();
            return t != null;
        } catch(Exception e){
            return false;
        }
    }

    @ReactMethod
    public void getConnectedReader(Promise promise) {
        try {
            Terminal terminal = Terminal.getInstance();
            Reader reader = terminal.getConnectedReader();

            if (reader != null) {
                WritableMap readerMap = readerToMap(reader);
                promise.resolve(readerMap);
            } else {
                promise.resolve(false);
            }
        } catch (Exception e) {
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void init(ReadableMap options, Promise promise) {

        if(_isInitialized()){
            promise.resolve(true);
            return;
        }

        if(Build.VERSION.SDK_INT < 21){
            promise.reject("Error","You need a more recent version of Android");
            return;
        }

        try {
            String url = options.getString("url");
            String authToken = options.getString("authToken");

            // Create your token provider.
            TokenProvider tokenProvider = new TokenProvider(url, authToken);

            TerminalListener listener = new TerminalEventListener(this);

            ReactApplicationContext reactContext = getReactApplicationContext();
            // Pass in the current application context, the desired log level, your token provider, and the listener you created
            Terminal.initTerminal(reactContext, LogLevel.VERBOSE, tokenProvider, listener);

            Terminal terminal = Terminal.getInstance();

            promise.resolve(true);

        } catch (TerminalException e) {
            promise.reject("TerminalException", e.getErrorMessage());
        } catch (Exception e){
            promise.reject("Exception", e.getMessage());
        }
    }

    @ReactMethod
    public void discoverReaders(int timeout, Promise promise) {

        if(!_isInitialized()){
            promise.reject("Error", "Terminal instance not initialized");
            return;
        }

        this.isDiscovering = true;

        try {
            DiscoveryConfiguration config = new DiscoveryConfiguration(timeout, DeviceType.CHIPPER_2X, false);
            DiscoveryEventListener discoveryEventListener = new DiscoveryEventListener(this);

            Terminal terminal = Terminal.getInstance();
            cancelableDiscovery = terminal.discoverReaders(
                config,
                discoveryEventListener,
                new DiscoveryCallback(this, promise)
            );
        } catch(Exception err){
            this.isDiscovering = false;
            cancelableDiscovery = null;
            promise.reject("Error",err.getMessage());
        }

        // todo - handle this promise in DiscoveryCallbacks?
        //promise.resolve(true);
    }

    @ReactMethod
    public void cancelDiscovery(Promise promise){
        if(cancelableDiscovery == null){
            promise.resolve(true);
        } else {
            if(cancelableDiscovery.isCompleted()){
                promise.resolve(true);
                cancelableDiscovery = null;
            } else {
                cancelableDiscovery.cancel(new DiscoveryCancellationCallback(this, promise));
            }
        }
    }


    @ReactMethod
    public void connectReader(String readerId, Promise promise) {

        Reader reader = findReaderBySerial(readerId);

        if (reader != null) {
            try {
                Terminal terminal = Terminal.getInstance();
                Reader connectedReader = terminal.getConnectedReader();
                if(connectedReader != null){
                    terminal.disconnectReader(new DisconnectCallback(this, null));
                }
                terminal.connectReader(reader, new ConnectionCallback(this, promise));
            } catch(Exception exception){
                promise.reject("Error",exception.getMessage());
            }
        } else {
            promise.reject("Error","Could not connect to reader");
        }
    }

    @ReactMethod
    public void disconnectReader(Promise promise) {

        Terminal terminal = Terminal.getInstance();
        Reader reader = terminal.getConnectedReader();
        if(reader != null){
            terminal.disconnectReader(new DisconnectCallback(this, promise));
        } else {
            promise.reject("Error","No reader connected");
        }
    }


    private Reader findReaderBySerial(String serial) {
        for (Reader reader : availableReaders) {
            if (reader.getSerialNumber().equals(serial)) return reader;
        }
        return null;
    }

    public void setAvailableReaders(List<Reader> list) {
        availableReaders = list;

        WritableArray wa = Arguments.createArray();

        for (Reader reader : availableReaders) {
            WritableMap readerMap = readerToMap(reader);
            wa.pushMap(readerMap);
        }

        emit("updateDiscoveredReaders", wa);
    }

    public WritableMap readerToMap(Reader reader) {
        WritableMap readerMap = Arguments.createMap();
        readerMap.putString("serial", reader.getSerialNumber());
        Float batt = reader.getBatteryLevel();
        if (batt != null) {
            readerMap.putDouble("batteryLevel", batt.doubleValue());
        }
        readerMap.putString("deviceType", reader.getDeviceType().name());

        return readerMap;
    }

    @ReactMethod
    public void readReusableCard(Promise promise){
        try {

            ReadReusableCardParameters params = new ReadReusableCardParameters.Builder()
                    .build();

            Terminal.getInstance().readReusableCard(params, this, new PaymentMethodCallback() {
                @Override
                public void onSuccess(PaymentMethod paymentMethod) {
                    promise.resolve(paymentMethodToMap(paymentMethod));
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    promise.reject("Error",e.getErrorMessage());
                }
            });

        } catch(Exception error){
            promise.reject("Error",error.getMessage());
        }
    }

    @ReactMethod
    public void createPaymentIntent(int amount, String currency, String statementDescriptor, Promise promise) {

        PaymentIntentParameters.Builder builder = new PaymentIntentParameters.Builder()
                .setAmount(amount)
                .setCurrency(currency);

        if(!statementDescriptor.isEmpty()) {
            builder.setStatementDescriptor(statementDescriptor);
        }

        PaymentIntentParameters params = builder.build();

        Terminal.getInstance().createPaymentIntent(params, new CreatePaymentIntentCallback(this, promise));
    }

    /**
     * Notify the `Activity` that a [PaymentIntent] has been created
     */
    public void onCreatePaymentIntent(PaymentIntent paymentIntent, Promise promise) {

        this.currentPaymentIntent = paymentIntent;
        WritableMap pi = paymentIntentToMap(paymentIntent);

        promise.resolve(pi);
    }

    @ReactMethod
    public void collectPaymentMethod(Promise promise) {
        if (this.currentPaymentIntent == null) {
            promise.reject("Error","No existing paymentIntent found");
            return;
        }

        this.cancelableCollect = Terminal.getInstance().collectPaymentMethod(
            this.currentPaymentIntent,
            this,
            new CollectPaymentMethodCallback(this, promise)
        );
    }


    @ReactMethod
    public void cancelCollectPaymentMethod(Promise promise){
        if(cancelableCollect == null){
            promise.reject("Error","Nothing to cancel");
        } else {
            if (!cancelableCollect.isCompleted()) {
                cancelableCollect.cancel(new CollectPaymentMethodCancellationCallback(this, promise));
            } else {
                promise.resolve(true);
                cancelableCollect = null;
            }
        }
    }

    /**
     * Notify the `Activity` that a payment method has been collected
     */
    public void onCollectPaymentMethod(PaymentIntent paymentIntent, Promise promise) {
        this.currentPaymentIntent = paymentIntent;

        WritableMap pi = paymentIntentToMap(paymentIntent);
        promise.resolve(pi);

    }

    @ReactMethod
    public void confirmPaymentIntent(Promise promise) {

        Terminal.getInstance().processPayment(
                currentPaymentIntent,
                new ConfirmPaymentIntentCallback(this, promise)
        );
    }

    /**
     * Notify the `Activity` that a [PaymentIntent] has been confirmed
     */
    public void onConfirmPaymentIntent(PaymentIntent paymentIntent, Promise promise) {
        // This is called from ConfirmPaymentIntentCallback which is passed into collectPaymentMethod
        this.currentPaymentIntent = paymentIntent;

        WritableMap pi = paymentIntentToMap(paymentIntent);

        promise.resolve(pi);

    }

    @Override
    public void onRequestReaderInput(ReaderInputOptions options) {
        // todo trigger event in react
        System.out.println("Reader requests input in one of the following methods: " +
                options.toString());

        emit("ReaderStatus", options.toString());

    }

    @Override
    public void onRequestReaderDisplayMessage(ReaderDisplayMessage prompt) {
        // Todo trigger event in react
        System.out.println("Reader prompts for the following action: " +
                prompt.toString());

        emit("ReaderStatus", prompt.toString());
    }

    /**
     * Notify the `Activity` that collecting payment method has been canceled
     */
    public void onCancelCollectPaymentMethod(Promise promise) {
        promise.resolve(true);
    }

    /**
     * Notify the `Activity` that discovery has been canceled
     */
    public void onCancelDiscovery(Promise promise) {
        promise.resolve(true);
        cancelableDiscovery = null;
    }

    /**
     * Notify the `Activity` that a [Reader] has been connected to
     */
    public void onConnectReader(Reader reader, Promise promise) {
        if(promise != null)
            promise.resolve(true);

        emit("ConnectionStatusChange", "CONNECTED");
    }

    /**
     * Notify the `Activity` that we've disconnected from all [Reader]s
     */
    public void onDisconnectReader(Promise promise) {
        if(promise != null)
            promise.resolve(true);
        emit("ConnectionStatusChange", "NOT_CONNECTED");
    }

    /**
     * Notify the `Activity` that [Reader] discovery has completed
     */
    public void onDiscoverReaders(Promise promise) {
        promise.resolve(true);
        this.isDiscovering = false;
        cancelableDiscovery = null;
    }




    /**
     * Notify the `Activity` that a [TerminalException] has been thrown
     */
    public void onFailure(TerminalException e) {
        emit("ReaderError",  e.getErrorCode().toString() +": "+e.getErrorMessage());
    }

    private WritableMap paymentIntentToMap(PaymentIntent paymentIntent) {
        WritableMap pi = Arguments.createMap();

        pi.putString("id", paymentIntent.getId());
        pi.putDouble("created", paymentIntent.getCreated());
        pi.putInt("amount", paymentIntent.getAmount());
        pi.putString("clientSecret", paymentIntent.getClientSecret());
        pi.putString("status", paymentIntent.getStatus().toString());

        return pi;
    }

    private WritableMap paymentMethodToMap(PaymentMethod paymentMethod){
        WritableMap pm = Arguments.createMap();
        pm.putString("id", paymentMethod.getId());
        pm.putString("customer", paymentMethod.getCustomer());

        PaymentMethod.CardDetails cardDetails = paymentMethod.getCardDetails();

        WritableMap cd = Arguments.createMap();
        cd.putString("brand", cardDetails.getBrand());
        cd.putString("country", cardDetails.getCountry());
        cd.putInt("expMonth", cardDetails.getExpMonth());
        cd.putInt("expYear", cardDetails.getExpYear());
        cd.putString("fingerprint", cardDetails.getFingerprint());
        cd.putString("last4", cardDetails.getLast4());

        pm.putMap("cardDetails", cd);

        return pm;

    }

    public void emit(String event, @Nullable Object data){

        Map <String, Object> m = new HashMap<String, Object>();
        m.put("event", event);
        m.put("data", data);

        WritableNativeMap returnObj = Arguments.makeNativeMap(m);

        Log.i("emit", returnObj.toString());

        getReactApplicationContext()
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("StripeTerminalEvent", returnObj);
    }

    @ReactMethod
    public void checkForUpdate(Promise promise){
        RNStripeTerminalModule manager = this;

        if(_isInitialized()){

            ReaderSoftwareUpdateCallback callback = new ReaderSoftwareUpdateCallback() {
                @Override
                public void onSuccess(ReaderSoftwareUpdate update) {
                    // todo implement
                    manager.availableUpdate = update;
                    promise.resolve(true);
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    promise.resolve(false);
                }
            };

            cancelableUpdate = Terminal.getInstance().checkForUpdate(callback);

        } else {
            promise.reject("TerminalException", "Terminal Instance not Initialized");
        }
    }

    @ReactMethod
    public void installUpdate(Promise promise){
        RNStripeTerminalModule manager = this;

        ReaderSoftwareUpdateListener listener = new ReaderSoftwareUpdateListener() {
            @Override
            public void onReportReaderSoftwareUpdateProgress(float progress) {
                emit("updateProgress", progress);
            }
        };

        Callback callback = new Callback() {
            @Override
            public void onSuccess() {
                manager.availableUpdate = null;
                promise.resolve(true);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                promise.reject("UpdateError", "The update failed to install");
            }
        };

        cancelableInstall = Terminal.getInstance().installUpdate(this.availableUpdate, listener, callback);

    }

}