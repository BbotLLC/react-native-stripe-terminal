package menu.bbot.reactnativestripeterminal;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.stripe.stripeterminal.external.models.Address;
import com.stripe.stripeterminal.external.models.CardDetails;
import com.stripe.stripeterminal.external.models.DeviceType;
import com.stripe.stripeterminal.external.models.Location;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.PaymentMethod;
import com.stripe.stripeterminal.external.models.Reader;

import java.util.HashMap;

public class Helpers {

    public static WritableMap ReaderToMap(Reader reader) {
        WritableMap readerMap = Arguments.createMap();
        readerMap.putString("serial_number", reader.getSerialNumber());
        Float batt = reader.getBatteryLevel();
        if (batt != null) {
            readerMap.putDouble("battery_level", batt.doubleValue());
        }
        readerMap.putString("device_type", reader.getDeviceType().name());
        readerMap.putBoolean("available_update", reader.getAvailableUpdate() != null);
        Location loc = reader.getLocation();
        if (loc != null) {
            readerMap.putString("location_id", loc.getId());
        }
        readerMap.putString("label", reader.getLabel());
        readerMap.putString("software_version", reader.getSoftwareVersion());
        readerMap.putString("base_url", reader.getBaseUrl());

        return readerMap;
    }

    public static HashMap<String, String> ReadableMapToHashMap(ReadableMap readableMap){
        ReadableMapKeySetIterator it = readableMap.keySetIterator();
        HashMap<String, String> hashMap = new HashMap<String,String>();
        while(it.hasNextKey()){
            String key = it.nextKey();
            hashMap.put(key, readableMap.getString(key));
        }
        return hashMap;
    }

    public static WritableMap PaymentIntentToMap(PaymentIntent paymentIntent) {
        WritableMap pi = Arguments.createMap();

        pi.putString("id", paymentIntent.getId());
        pi.putDouble("created", paymentIntent.getCreated());
        pi.putInt("amount", (int) paymentIntent.getAmount());
        pi.putString("clientSecret", paymentIntent.getClientSecret());
        pi.putString("status", paymentIntent.getStatus().toString());

        return pi;
    }

    public static WritableMap PaymentMethodToMap(PaymentMethod paymentMethod) {
        WritableMap pm = Arguments.createMap();
        pm.putString("id", paymentMethod.getId());
        pm.putString("customer", paymentMethod.getCustomer());

        CardDetails cardDetails = paymentMethod.getCardDetails();

        if(cardDetails != null) {
            WritableMap cd = Arguments.createMap();
            cd.putString("brand", cardDetails.getBrand());
            cd.putString("country", cardDetails.getCountry());
            cd.putInt("expMonth", cardDetails.getExpMonth());
            cd.putInt("expYear", cardDetails.getExpYear());
            cd.putString("fingerprint", cardDetails.getFingerprint());
            cd.putString("last4", cardDetails.getLast4());

            pm.putMap("cardDetails", cd);
        }

        return pm;

    }

    public static WritableMap LocationToMap(Location location) {
        WritableMap locationMap = Arguments.createMap();
        locationMap.putString("id", location.getId());
        locationMap.putString("displayName", location.getDisplayName());
        locationMap.putBoolean("livemode", location.getLivemode());

        Address address = location.getAddress(); // city, country, line1, line2, postalCode, state
        if(address != null) {
            WritableMap addressMap = Arguments.createMap();
            addressMap.putString("city", address.getCity());
            addressMap.putString("country", address.getCountry());
            addressMap.putString("line1", address.getLine1());
            addressMap.putString("line2", address.getLine2());
            addressMap.putString("postalCode", address.getPostalCode());
            addressMap.putString("state", address.getState());

            locationMap.putMap("address", addressMap);
        }

        return locationMap;
    }


    public static WritableArray EnumToArray(Enum theEnum){
        WritableArray result = Arguments.createArray();

        // Todo: Write magic
        return result;
    }

}
