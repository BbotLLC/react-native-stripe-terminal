using ReactNative.Bridge;
using System;
using System.Collections.Generic;
using Windows.ApplicationModel.Core;
using Windows.UI.Core;

namespace React.Native.Stripe.Terminal.RNReactNativeStripeTerminal
{
    /// <summary>
    /// A module that allows JS to share data.
    /// </summary>
    class RNReactNativeStripeTerminalModule : NativeModuleBase
    {
        /// <summary>
        /// Instantiates the <see cref="RNReactNativeStripeTerminalModule"/>.
        /// </summary>
        internal RNReactNativeStripeTerminalModule()
        {

        }

        /// <summary>
        /// The name of the native module.
        /// </summary>
        public override string Name
        {
            get
            {
                return "RNReactNativeStripeTerminal";
            }
        }
    }
}
