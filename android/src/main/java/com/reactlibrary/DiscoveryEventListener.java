package com.reactlibrary;

import com.reactlibrary.RNReactNativeStripeTerminalModule;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.stripe.stripeterminal.DiscoveryListener;
import com.stripe.stripeterminal.Reader;
import com.stripe.stripeterminal.Terminal;
import java.util.HashMap;
import java.util.List;

public class DiscoveryEventListener implements DiscoveryListener {

    private final RNReactNativeStripeTerminalModule module;

    public DiscoveryEventListener(RNReactNativeStripeTerminalModule module){
        this.module = module;
    }


    public void onUpdateDiscoveredReaders(List<Reader> readers){

        module.setAvailableReaders(readers);

    }

}