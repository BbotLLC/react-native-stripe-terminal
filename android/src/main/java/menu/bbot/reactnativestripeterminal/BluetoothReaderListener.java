package menu.bbot.reactnativestripeterminal;

import android.util.Log;

import com.stripe.stripeterminal.external.callable.BluetoothReaderListener;
import com.stripe.stripeterminal.external.models.TerminalException;
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate;
import com.stripe.stripeterminal.external.models.ReaderEvent;
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage;
import com.stripe.stripeterminal.external.models.ReaderInputOptions;

public class BluetoothReaderEventListener implements BluetoothReaderListener {

    private final RNStripeTerminalModule manager;

    public BluetoothReaderEventListener (RNStripeTerminalModule manager){
        super();
        this.manager = manager;
    }

    public onFinishInstallingUpdate(ReaderSoftwareUpdate, TerminalException e){

    }

    public onReportAvailableUpdate(ReaderSoftwareUpdate update){

    }

    public onReportLowBatteryWarning(){
        manager.emit("LowBatteryWarning", true)
    }

    public onReportReaderEvent(ReaderEvent event){
        manager.emit("ReportReaderEvent", event)
    }

    // The terminal reported progress on a reader software update.
    public onReportReaderSoftwareUpdateProgress(Float progress){
        manager.emit("ReaderSoftwareUpdateProgress", progress.toString()
    }

    // This method is called to request that a message be displayed in your app.
    public onRequestReaderDisplayMessage(ReaderDisplayMessage message){
        manager.emit("RequestReaderDisplayMessage", message.toString()
    }

    // ReaderInputOptions.ReaderInputOption (TAP, SWIPE, INSERT, NONE)
    public onRequestReaderInput(ReaderInputOptions options){
        manager.emit("RequestReaderInput", options.toString()
    }

    // The SDK is reporting that the reader has started installation of a required update that must be completed before the reader can be used.
    public onStartInstallingUpdate(ReaderSoftwareUpdate update, Cancelable cancelable){

        manager.emit("StartInstallingUpdate")
    }

}
