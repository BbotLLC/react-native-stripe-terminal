
package menu.bbot.reactnativestripeterminal;

import android.app.Application;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;
import com.stripe.stripeterminal.TerminalApplicationDelegate;


public class RNStripeTerminalPackage implements ReactPackage {

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
      return Arrays.<NativeModule>asList(
          new RNStripeTerminalModule(reactContext)
      );
    }

    // Deprecated from RN 0.47
    public List<Class<? extends JavaScriptModule>> createJSModules() {
      return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
      return Collections.emptyList();
    }

    public static void onCreate(Application app) {
        TerminalApplicationDelegate.onCreate(app);
    }

    public static void onTrimMemory(Application app, int level) {
        TerminalApplicationDelegate.onTrimMemory(app, level);
    }
}