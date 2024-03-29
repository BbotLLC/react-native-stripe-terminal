package menu.bbot.reactnativestripeterminal;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.stripe.stripeterminal.external.callable.Cancelable;

import com.stripe.stripeterminal.external.callable.BluetoothReaderListener;
import com.stripe.stripeterminal.external.callable.UsbReaderListener;
import com.stripe.stripeterminal.external.models.BatteryStatus;
import com.stripe.stripeterminal.external.models.TerminalException;
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate;
import com.stripe.stripeterminal.external.models.ReaderEvent;
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage;
import com.stripe.stripeterminal.external.models.ReaderInputOptions;

class BluetoothReaderEventListener implements BluetoothReaderListener, UsbReaderListener {

    private final RNStripeTerminalModule manager;

    public BluetoothReaderEventListener (RNStripeTerminalModule manager){
        super();
        this.manager = manager;
    }

    public void onFinishInstallingUpdate(ReaderSoftwareUpdate update, TerminalException e){
        manager.emit("FinishedInstallingUpdate", null);
        manager.updatePromise.resolve(true);
    }

    public void onReportAvailableUpdate(@NonNull ReaderSoftwareUpdate update){
        manager.emit("UpdateAvailable", true);
    }

    public void onReportLowBatteryWarning(){
        manager.emit("LowBatteryWarning", null);
    }

    public void onReportReaderEvent(ReaderEvent event){
        manager.emit("ReaderEvent", event.toString());
    }

    // The terminal reported progress on a reader software update.
    public void onReportReaderSoftwareUpdateProgress(float progress){
        manager.emit("ReaderSoftwareUpdateProgress", progress);
    }

    // This method is called to request that a message be displayed in your app.
    public void onRequestReaderDisplayMessage(ReaderDisplayMessage message){
        //manager.emit("RequestReaderDisplayMessage", message.toString());
        manager.emit("ReaderStatus", message.toString());
    }

    // ReaderInputOptions.ReaderInputOption (TAP, SWIPE, INSERT, NONE)
    public void onRequestReaderInput(ReaderInputOptions options){
        //manager.emit("RequestReaderInput", options.toString());
        manager.emit("ReaderStatus", options.toString());
    }

    // The SDK is reporting that the reader has started installation of a required update that must be completed before the reader can be used.
    public void onStartInstallingUpdate(ReaderSoftwareUpdate update, Cancelable cancelable){
        manager.emit("StartInstallingUpdate", true);
    }

    @Override
    public void onBatteryLevelUpdate(float batteryLevel, @NonNull BatteryStatus batteryStatus, boolean isCharging) {
        WritableMap map = Arguments.createMap();
        map.putDouble("batteryLevel", batteryLevel);
        map.putString("batteryStatus", batteryStatus.name());
        map.putBoolean("isCharging", isCharging);
        manager.emit("BatteryLevelUpdate", map);
    }
}
