## Getting started
`$ npm install BbotLLC/react-native-stripe-terminal --save`

#### Android
Open up `android/app/src/main/java/[...]/MainApplication.java`
   - Add `import menu.bbot.reactnativestripeterminal.RNStripeTerminalPackage;` to the imports at the top of the file
   - Add or update the `onCreate` and `onTrimMemory` functions to resemble the following:
   ```
   @Override
   public void onCreate() {
     super.onCreate();
     RNStripeTerminalPackage.onCreate(this); 
   }

   @Override
   public void onTrimMemory(int level) {
     super.onTrimMemory(level);
     RNStripeTerminalPackage.onTrimMemory(this, level);
   }
   ```

### Manual installation (for older versions of react) 
#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNReactNativeStripeTerminalPackage;` to the imports at the top of the file
  - Add `new RNReactNativeStripeTerminalPackage()` to the list returned by the `getPackages()` method



3. Append the following lines to `android/settings.gradle`:
      ```
      include ':react-native-stripe-terminal'
      project(':react-native-stripe-terminal').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-stripe-terminal/android')
      ```
4. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
      ```
      compile project(':react-native-stripe-terminal')
      ```


## Usage
```javascript
import Stripe from 'react-native-stripe-terminal';

const startStripe = async () => {
	await Stripe.init({
		fetchConnectionToken: API.getStripeConnectionToken,
		createPaymentIntent: API.createStripePaymentIntent,  // required for Internet Terminals
		autoReconnect: true
	});
}
```

See `src/StripeTerminal.js` for Web functionality and `src/StripeTerminal.android.js` for Android.
Most API calls are cross-compatible (when applicable);
