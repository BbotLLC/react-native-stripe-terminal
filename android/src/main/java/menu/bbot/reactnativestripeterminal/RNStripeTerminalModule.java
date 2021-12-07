package menu.bbot.reactnativestripeterminal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
//import com.facebook.react.bridge.Callback; // https://reactnative.dev/docs/native-modules-android#callbacks
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.models.SimulateReaderUpdate;
import com.stripe.stripeterminal.external.models.SimulatedCard;
import com.stripe.stripeterminal.external.models.SimulatedCardType;
import com.stripe.stripeterminal.external.models.SimulatorConfiguration;
import com.stripe.stripeterminal.log.LogLevel;
import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.LocationListCallback;
import com.stripe.stripeterminal.external.callable.PaymentMethodCallback;
import com.stripe.stripeterminal.external.callable.TerminalListener;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;

import com.stripe.stripeterminal.external.api.ApiError;
import com.stripe.stripeterminal.external.models.Address;
import com.stripe.stripeterminal.external.models.CardDetails;
import com.stripe.stripeterminal.external.models.DeviceType;
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration;
import com.stripe.stripeterminal.external.models.DiscoveryMethod;
import com.stripe.stripeterminal.external.models.Location;
import com.stripe.stripeterminal.external.models.ListLocationsParameters;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.PaymentIntentParameters;
import com.stripe.stripeterminal.external.models.PaymentMethod;
import com.stripe.stripeterminal.external.models.ReadReusableCardParameters;
import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage;
import com.stripe.stripeterminal.external.models.ReaderInputOptions;
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate;
import com.stripe.stripeterminal.external.models.TerminalException;

import com.stripe.stripeterminal.external.models.ConnectionConfiguration.BluetoothConnectionConfiguration;
import com.stripe.stripeterminal.external.models.ConnectionConfiguration.InternetConnectionConfiguration;
import com.stripe.stripeterminal.external.models.ConnectionConfiguration.EmbeddedConnectionConfiguration;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import java.lang.Enum;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;

import menu.bbot.reactnativestripeterminal.callbacks.CollectPaymentMethodCallback;
import menu.bbot.reactnativestripeterminal.callbacks.ConfirmPaymentIntentCallback;
import menu.bbot.reactnativestripeterminal.callbacks.ConnectionCallback;
import menu.bbot.reactnativestripeterminal.callbacks.CreatePaymentIntentCallback;
import menu.bbot.reactnativestripeterminal.callbacks.DisconnectCallback;
import menu.bbot.reactnativestripeterminal.callbacks.DiscoveryCallback;
import menu.bbot.reactnativestripeterminal.callbacks.DiscoveryCancellationCallback;

public class RNStripeTerminalModule
        extends ReactContextBaseJavaModule
        implements TerminalStateManager {

    private static final String TAG = "RNStripeTerminal";

    private List<? extends Reader> availableReaders;

    private Boolean discoveryInProgress = false;
    private Cancelable cancelableDiscovery;
    private Cancelable cancelableCollect;
    private Cancelable cancelableUpdate;
    private Cancelable cancelableInstall;
    private Cancelable cancelableReusable;

    private Promise connectionPromise;
    public Promise updatePromise;
    private PaymentIntent currentPaymentIntent;
    private TokenProvider tokenProvider;

    public RNStripeTerminalModule(ReactApplicationContext reactContext) {
        super(reactContext);

        availableReaders = new ArrayList<Reader>();
    }

    @Override
    public String getName() {
        // The name you use when importing the module from NativeModules
        return "StripeTerminal";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();

        // Output Enum to Array of String:
        List<String> discoveryMethodNames = Arrays.stream(DiscoveryMethod.values())
                .map(e -> e.name())
                .collect(Collectors.toList());

     /*   WritableArray simulatedCardTypes = Arguments.createArray();
        for(SimulatedCardType sm : SimulatedCardType.values()){
            WritableMap methodMap = Arguments.createMap();
            methodMap.putInt("ordinal", sm.ordinal());
            methodMap.putString("name", sm.name());
            simulatedCardTypes.pushMap(methodMap);
        }*/

        WritableArray discoveryMethods = Arguments.createArray();
        for(DiscoveryMethod dm : DiscoveryMethod.values()){
            WritableMap methodMap = Arguments.createMap();
            methodMap.putInt("ordinal", dm.ordinal());
            methodMap.putString("name", dm.name());
            discoveryMethods.pushMap(methodMap);
        }

        WritableArray deviceTypes = Arguments.createArray();
        for(DeviceType dt : DeviceType.values()){
            WritableMap deviceMap = Arguments.createMap();
            deviceMap.putString("device_name", dt.getDeviceName());
            deviceMap.putString("name", dt.name());
            deviceMap.putInt("ordinal", dt.ordinal());
            deviceMap.putArray("serial_prefixes", Arguments.fromArray(dt.getSerialPrefixes()
                    .stream().toArray(String[]::new)));
            deviceTypes.pushMap(deviceMap);
        }

        constants.put("DiscoveryMethods", discoveryMethods);
        constants.put("DeviceTypes", deviceTypes);

        return constants;
    }

    @ReactMethod
    public void isInitialized(Promise promise) {
        promise.resolve(Terminal.isInitialized());
    }

    @ReactMethod
    public void init(ReadableMap options, Promise promise) {

        if (Terminal.isInitialized()) {
            promise.resolve(true);
            return;
        }

        try {
            // Create your token provider.
            tokenProvider = new TokenProvider(this);

            TerminalListener listener = new TerminalEventListener(this);

            ReactApplicationContext reactContext = getReactApplicationContext();
            // Pass in the current application context, the desired log level, your token provider, and the listener you created
            Terminal.initTerminal(reactContext, LogLevel.VERBOSE, tokenProvider, listener);

            Terminal terminal = Terminal.getInstance();

            promise.resolve(true);

        } catch (TerminalException e) {
            promise.reject("TerminalException", e.getErrorMessage());
        } catch (Exception e) {
            promise.reject("Exception", e.getMessage());
        }
    }

    @ReactMethod
    public void setConnectionToken(String token, String errorMsg, Promise promise) {
        if(tokenProvider.callback != null){
            if(errorMsg!=null && !errorMsg.trim().isEmpty()){
                tokenProvider.callback.onFailure(new ConnectionTokenException(errorMsg));
            }else{
                tokenProvider.callback.onSuccess(token);
            }
        }
        tokenProvider.callback = null;
    }

    @ReactMethod
    public void getConnectedReader(Promise promise) {
        if (!Terminal.isInitialized()) {
            promise.resolve(false);
            return;
        }
        try {
            Terminal terminal = Terminal.getInstance();
            Reader reader = terminal.getConnectedReader();

            if (reader != null) {
                WritableMap readerMap = Helpers.ReaderToMap(reader);
                promise.resolve(readerMap);
            } else {
                promise.resolve(false);
            }
        } catch (Exception e) {
            promise.resolve(false);
        }
    }

    @ReactMethod
    public void discoverReaders(ReadableMap options, Promise promise) {
        Log.i(TAG, "discoverReaders");

        if (!Terminal.isInitialized()) {
            promise.reject("Error", "Terminal instance not initialized");
            return;
        }
        Terminal terminal = Terminal.getInstance();
        Reader connectedReader = terminal.getConnectedReader();

        if (connectedReader != null) {
            terminal.disconnectReader(new DisconnectCallback(this, null));
        }

        int timeout = options.hasKey("timeout") ? options.getInt("timeout") : 0;
        boolean simulated = options.hasKey("simulated") && options.getBoolean("simulated");
        String locationId = options.getString("locationId");

        DiscoveryMethod discoveryMethod = DiscoveryMethod.BLUETOOTH_SCAN; // default

        if (options.hasKey("discoveryMethod") ) {
            discoveryMethod = DiscoveryMethod.values()[options.getInt("discoveryMethod")];
        }

        discoveryInProgress = true;

        Log.i(TAG, String.format("discovering Readers: Timeout: %d, Method: %s, Simulated: %b", timeout, discoveryMethod.toString(), simulated));

        try {
            DiscoveryConfiguration config = new DiscoveryConfiguration(
                    timeout,
                    discoveryMethod,
                    simulated
            );
            DiscoveryEventListener discoveryEventListener = new DiscoveryEventListener(this);

            cancelableDiscovery = Terminal.getInstance().discoverReaders(
                    config,
                    discoveryEventListener,
                    new DiscoveryCallback(this, promise)
            );

            emit("Discovering", true);
        } catch (Exception err) {
            discoveryInProgress = false;
            cancelableDiscovery = null;
            Log.i(TAG, "Caught error in discoverReaders");
            promise.reject("Error", err.getMessage());
        }

        // todo - handle this promise in DiscoveryCallbacks?
        //promise.resolve(true);
    }

    @ReactMethod
    public void isDiscovering(Promise promise) {
        promise.resolve(discoveryInProgress);
    }

    @ReactMethod
    public void cancelDiscovery(Promise promise) {
        Log.i(TAG, "Cancel Discovery");
        if (cancelableDiscovery == null) {
            promise.resolve(true);
        } else {
            if (cancelableDiscovery.isCompleted()) {
                promise.resolve(true);
                cancelableDiscovery = null;
            } else {
                cancelableDiscovery.cancel(new DiscoveryCancellationCallback(this, promise));
            }
        }
    }


    @ReactMethod
    public void connectBluetoothReader(String readerId, ReadableMap options, Promise promise) {

        Reader reader = findReaderBySerial(readerId);
        Location readerLocation = reader.getLocation();

        connectionPromise = promise;

        String locationId = options.getString("locationId");
        if (locationId == null) {
            if(readerLocation != null) {
                locationId = reader.getLocation().getId();
            } else {
                locationId = "tml_EPf4XQx4i4rtHP"; // TODO: this should be pulled from discovery location id
            }
        }

        if (reader != null) {
            try {
                Terminal terminal = Terminal.getInstance();
                Reader connectedReader = terminal.getConnectedReader();
                if (connectedReader != null) {
                    terminal.disconnectReader(new DisconnectCallback(this, null));
                }
                terminal.connectBluetoothReader(
                        reader,
                        new BluetoothConnectionConfiguration(locationId),
                        new BluetoothReaderEventListener(this),
                        new ConnectionCallback(this, promise)
                );
            } catch (Exception exception) {
                promise.reject("Error", exception.getMessage());
            }
        } else {
            promise.reject("Error", "Could not connect to reader");
        }
    }

    @ReactMethod
    public void connectInternetReader(String readerId, Promise promise) {
        Reader reader = findReaderBySerial(readerId);
        Log.i(TAG, "connectInternetReader: "+readerId);

        if (reader == null) {
            promise.reject("Error", "Error connecting to reader. Please try again");
        } else {
            try {
                Reader connectedReader = Terminal.getInstance().getConnectedReader();
                if (connectedReader != null) {
                    Log.i(TAG, "disconnecting already connected reader: "+connectedReader.getSerialNumber());
                    Terminal.getInstance().disconnectReader(new DisconnectCallback(this, null));
                }
                Terminal.getInstance().connectInternetReader(
                        reader,
                        new InternetConnectionConfiguration(),
                        new ReaderCallback() {
                            @Override
                            public void onSuccess(@NonNull Reader successReader){
                                Log.i(TAG, "successReader" + successReader.toString());
                                Log.i(TAG, "Successfully connected to reader "+reader.getSerialNumber());
                                onConnectReader(reader, promise);
                            }

                            @Override
                            public void onFailure(TerminalException e){
                                promise.reject("ConnectionError", e.getErrorMessage());
                                onFailure(e);
                            }
                        }
                );
            } catch (Exception exp) {
                promise.reject("ConnectionError", exp.getMessage());
            }
        }
    }

    @ReactMethod
    public void connectEmbeddedReader(String readerId, Promise promise) {
        Reader reader = findReaderBySerial(readerId);
        connectionPromise = promise;

        if (reader == null) {
            promise.reject("Error", "Error connecting to reader. Please try again");
        } else {
            try {
                Reader connectedReader = Terminal.getInstance().getConnectedReader();
                if (connectedReader != null) {
                    Terminal.getInstance().disconnectReader(new DisconnectCallback(this, null));
                }
                Terminal.getInstance().connectEmbeddedReader(
                        reader,
                        new EmbeddedConnectionConfiguration(this),
                        new ConnectionCallback(this, promise)
                );
            } catch (Exception exp) {

            }
        }
    }

    @ReactMethod
    public void disconnectReader(Promise promise) {

        Terminal terminal = Terminal.getInstance();
        Reader reader = terminal.getConnectedReader();
        if (reader != null) {
            terminal.disconnectReader(new DisconnectCallback(this, promise));
        } else {
            promise.reject("Error", "No reader connected");
        }
    }


    private Reader findReaderBySerial(String serial) {
        for (Reader reader : availableReaders) {
            if (reader.getSerialNumber().equals(serial)) return reader;
        }
        return null;
    }

    private WritableArray getReadersArray(@NotNull List<? extends Reader> list) {
        WritableArray wa = Arguments.createArray();
        for (Reader reader : availableReaders) {
            WritableMap readerMap = Helpers.ReaderToMap(reader);
            wa.pushMap(readerMap);
        }
        return wa;
    }

    public void setAvailableReaders(@NotNull List<? extends Reader> list) {
        Log.i(TAG, "got available readers");
        availableReaders = list;

        WritableArray wa = getReadersArray(availableReaders);

        emit("UpdateDiscoveredReaders", wa);
    }

    @ReactMethod
    public void getAvailableReaders(Promise promise) {
        WritableArray wa = getReadersArray(availableReaders);
        promise.resolve(wa);
    }

    @ReactMethod
    public void setSimulatorConfiguration(ReadableMap options) {
        Terminal terminal = Terminal.getInstance();
        SimulatedCard card = new SimulatedCard(options.getString("testCardNumber"));
        terminal.setSimulatorConfiguration(new SimulatorConfiguration(SimulateReaderUpdate.NONE, card));
    }


    @ReactMethod
    public void readReusableCard(Promise promise) {
        try {
            Terminal terminal = Terminal.getInstance();
            ReadReusableCardParameters params = new ReadReusableCardParameters.Builder()
                    .build();
            if(cancelableReusable != null){

            }

            cancelableReusable = terminal.readReusableCard(params, new PaymentMethodCallback() {
                @Override
                public void onSuccess(@Nonnull PaymentMethod paymentMethod) {
                    promise.resolve(Helpers.PaymentMethodToMap(paymentMethod));
                    cancelableReusable = null;
                }

                @Override
                public void onFailure(@Nonnull TerminalException e) {
                    Log.i(TAG, "ReadReusableCard:onFailure");
                    promise.reject("Error", e.getErrorMessage());
                }
            });

        } catch (Exception error) {

            promise.reject("readReusableCardError", error.getMessage());
        }
    }

    @ReactMethod
    public void cancelReadReusableCard(Promise promise) {
        if (cancelableReusable == null) {
            promise.reject("Error", "Nothing to cancel");
        } else {
            if (!cancelableReusable.isCompleted()) {
                cancelableReusable.cancel(new Callback() {
                    @Override
                    public void onSuccess() {
                        promise.resolve(true);
                    }
                    @Override
                    public void onFailure(@Nonnull TerminalException e) {
                        promise.reject("CancelReadReusableCardError", e.getErrorMessage());
                    }
                });
            } else {
                promise.resolve(true);
                cancelableReusable = null;
            }
        }
    }


    @ReactMethod
    public void createPaymentIntent(ReadableMap parameters, Promise promise) {

        PaymentIntentParameters.Builder builder = new PaymentIntentParameters.Builder();
        if(parameters.hasKey("amount")){
            builder.setAmount((long) parameters.getInt("amount"));
        }
        if(parameters.hasKey("currency")){
            builder.setCurrency(parameters.getString("currency"));
        }
        if(parameters.hasKey("statement_descriptor")){
            builder.setStatementDescriptor(parameters.getString("statement_descriptor"));
        }
        if(parameters.hasKey("setup_future_usage")){
            builder.setSetupFutureUsage(parameters.getString("setup_future_usage"));
        }
        if(parameters.hasKey("on_behalf_of")){
            builder.setOnBehalfOf(parameters.getString("on_behalf_of"));
        }
        if(parameters.hasKey("metadata")){
            ReadableMap md = parameters.getMap("metadata");
            HashMap<String, String> metadata = Helpers.ReadableMapToHashMap(md);
            builder.setMetadata(metadata);
        }


        PaymentIntentParameters params = builder.build();
        RNStripeTerminalModule parent = this;

        Terminal.getInstance().createPaymentIntent(params, new PaymentIntentCallback() {
            @Override
            public void onSuccess(@NonNull PaymentIntent paymentIntent) {
                onCreatePaymentIntent(paymentIntent, promise);
            }

            @Override
            public void onFailure(TerminalException e) {
                promise.reject("CreatePaymentIntentError", e.getErrorMessage());
                parent.onFailure(e);
            }
        });
    }

    /**
     * Notify the `Activity` that a [PaymentIntent] has been created
     */
    public void onCreatePaymentIntent(PaymentIntent paymentIntent, Promise promise) {

        this.currentPaymentIntent = paymentIntent;
        WritableMap pi = Helpers.PaymentIntentToMap(paymentIntent);

        promise.resolve(pi);
    }

    @ReactMethod
    public void retrievePaymentIntent(String clientSecret, Promise promise) {
        Terminal.getInstance().retrievePaymentIntent(clientSecret,
            new PaymentIntentCallback() {
                @Override
                public void onSuccess(@NonNull PaymentIntent paymentIntent) {
                    currentPaymentIntent = paymentIntent;
                    promise.resolve(Helpers.PaymentIntentToMap(paymentIntent));
                }

                @Override
                public void onFailure(@NonNull TerminalException e) {
                    promise.reject("RETRIEVE_FAILED", "Error retrieving the payment intent");
                }
            }
        );
    }

    @ReactMethod
    public void collectPaymentMethod(Promise promise) {
        if (this.currentPaymentIntent == null) {
            promise.reject("Error", "No existing paymentIntent found");
            return;
        }

        this.cancelableCollect = Terminal.getInstance().collectPaymentMethod(
                this.currentPaymentIntent,
                new CollectPaymentMethodCallback(this, promise)
        );
    }


    @ReactMethod
    public void cancelCollectPaymentMethod(Promise promise) {
        if (cancelableCollect == null) {
            promise.reject("Error", "Nothing to cancel");
        } else {
            if (!cancelableCollect.isCompleted()) {
                cancelableCollect.cancel(new Callback() {
                    @Override
                    public void onSuccess() {
                        promise.resolve(true);
                    }
                    @Override
                    public void onFailure(@Nonnull TerminalException e) {
                        promise.reject("CancelCollectPaymentMethodError", e.getErrorMessage());
                    }
                });
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

        WritableMap pi = Helpers.PaymentIntentToMap(paymentIntent);
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

        WritableMap pi = Helpers.PaymentIntentToMap(paymentIntent);

        promise.resolve(pi);

    }

  /* DEPRECATED IN v2
    @Override
    public void onRequestReaderInput(ReaderInputOptions options) {
        // todo trigger event in react
        System.out.println("Reader requests input in one of the following methods: " +
                options.toString());

        emit("ReaderStatus", options.toString());

    }*/

    /* DEPRECATED IN V2
    @Override
    public void onRequestReaderDisplayMessage(ReaderDisplayMessage prompt) {
        // Todo trigger event in react
        System.out.println("Reader prompts for the following action: " +
                prompt.toString());

        emit("ReaderStatus", prompt.toString());
    }*/


    /**
     * Notify the `Activity` that discovery has been canceled
     */
    public void onCancelDiscovery(Promise promise) {
        promise.resolve(true);
        emit("DiscoveryCancelled", true);
        emit("Discovering", false);
        discoveryInProgress = false;
        cancelableDiscovery = null;
    }

    /**
     * Notify the `Activity` that a [Reader] has been connected to
     */
    public void onConnectReader(Reader reader, Promise promise) {
        promise.resolve(Helpers.ReaderToMap(reader));
        connectionPromise = null;
        discoveryInProgress = false;

        Log.i(TAG, "onConnectReader called");
        emit("ConnectionStatusChange", "CONNECTED");
    }

    /**
     * Notify the `Activity` that we've disconnected from all [Reader]s
     */
    public void onDisconnectReader(Promise promise) {
        if (promise != null)
            promise.resolve(true);

        Log.i(TAG, "onDisconnectReader called");
        emit("ConnectionStatusChange", "NOT_CONNECTED");
    }

    /**
     * Notify the `Activity` that [Reader] discovery has completed.*
     */
    public void onDiscoverReaders(Promise promise) {
        promise.resolve(true);
        emit("Discovering", false);
        discoveryInProgress = false;
        cancelableDiscovery = null;
    }


    @ReactMethod
    public void installAvailableUpdate(Promise promise) {
        updatePromise = promise;
        Terminal.getInstance().installAvailableUpdate();
    }

    @ReactMethod
    public void listLocations(ReadableMap options, Promise promise) {
        ListLocationsParameters.Builder builder = new ListLocationsParameters.Builder();
        builder.setLimit(100);

        // params.setEndingBefore()
        // params.setStartingAfter()

        Terminal.getInstance().listLocations(
                builder.build(),
                new LocationListCallback() {
                    @Override
                    public void onSuccess(@NotNull List<Location> locations, boolean hasMore) {
                        Log.i(TAG, "Found Locations!");
                        WritableArray wa = Arguments.createArray();
                        for (Location location : locations) {
                            WritableMap lm = Helpers.LocationToMap(location);
                            wa.pushMap(lm);
                        }

                        promise.resolve(wa);
                    }

                    @Override
                    public void onFailure(@NotNull TerminalException e) {
                        Log.i(TAG, "Error fetching locations: " + e.getErrorMessage());
                        promise.resolve(false);
                        // promise.reject()
                    }
                }
        );
    }

    /**
     * Notify the `Activity` that a [TerminalException] has been thrown
     */
    public void onFailure(@NotNull TerminalException e) {
        emit("ReaderError", e.getErrorCode().toString() + ": " + e.getErrorMessage());
        ApiError apiError = e.getApiError();
        if(apiError != null){
            emit("ApiError", apiError.getMessage());
        }
    }




    public void emit(String event, @Nullable Object data) {
        Log.i(TAG, "emit: " +event+":" + ( data != null ? data.toString() : ""));
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("event", event);
        m.put("data", data);
        
        WritableNativeMap returnObj = Arguments.makeNativeMap(m);

        Log.i("emit again", returnObj.toString());

        // For some reason, onConnectReader sometimes doesn't get called on successful connection
        // This lets us manually resolve the promise
        // (Likely because our React Native Bridge gets destroyed on refresh)
        if (event.equals("ConnectionStatusChange") && data.equals("CONNECTED") && connectionPromise != null) {
            Reader reader = Terminal.getInstance().getConnectedReader();
            if (reader != null) {
                WritableMap readerMap = Helpers.ReaderToMap(reader);
                connectionPromise.resolve(readerMap);
            } else {
                connectionPromise.reject("TerminalException", "Stripe says reader connected but no reader found");
            }
        }

        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("StripeTerminalEvent", returnObj);
    }

    /* DEPRECATED
    @ReactMethod
    public void checkForUpdate(Promise promise){
        if(Terminal.isInitialized()){

            ReaderSoftwareUpdateCallback callback = new ReaderSoftwareUpdateCallback() {
                @Override
                public void onSuccess(ReaderSoftwareUpdate update) {
                    availableUpdate = update;
                    promise.resolve(update != null);
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
    }*/

   /* DEPRECATED
   @ReactMethod
    public void installUpdate(Promise promise){

        if(availableUpdate == null) {
            promise.reject("UpdateError", "An internal error occurred. Please contact support");
            return;
        }

        ReaderSoftwareUpdateListener listener = new ReaderSoftwareUpdateListener() {
            @Override
            public void onReportReaderSoftwareUpdateProgress(float progress) {
                emit("updateProgress", progress);
            }
        };

        Callback callback = new Callback() {
            @Override
            public void onSuccess() {
                availableUpdate = null;
                promise.resolve(true);
            }

            @Override
            public void onFailure(@Nonnull TerminalException e) {
                promise.reject("UpdateError", "The update failed to install");
            }
        };

        cancelableInstall = Terminal.getInstance().installUpdate(availableUpdate, listener, callback);

    }*/

}
