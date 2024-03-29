import {loadStripeTerminal} from "@stripe/terminal-js";
import constants from "./constants";
import _ from "lodash";

// Web Implementation of StripeTerminal

class RNStripeTerminal {

  DiscoveryMethods = {
    INTERNET: 1
  }
  AvailableMethods = ['INTERNET']

  PaymentStatusMap = {
    waiting_for_input: "Waiting for Input",
    ready: "Ready",
    processing: "Processing",
  }

  _events = {}
  terminal = null
  connectedReader = null;

  settings = {
    fetchConnectionToken: () => {
      throw "onFetchConnectionToken was not initialized"
    },
    /**
     * createPaymentIntent should return a dict with `clientSecret` as the key
     */
    createPaymentIntent: () => {
      throw "createPaymentIntent is undefined. Must be initialized"
    },
    locationId: null,
    defaultReader: null
  }

  _lastConnectedReader = null;

  isInitialized = async () => {
    return !!this.terminal;
  }

  init = async (settings) => {

    // Override current settings with new settings:
    this.settings = Object.assign({}, this.settings, settings);

    if (this.terminal) return;

    const StripeTerminal = await loadStripeTerminal();

    this.terminal = StripeTerminal.create({
      onFetchConnectionToken: () => this.settings.fetchConnectionToken(),
      onUnexpectedReaderDisconnect: () => {
        this.connectedReader = null;
        if (this.settings.autoReconnect && this._lastConnectedReader) {
          try {
            this.discoverReaders(this.settings);
          } catch(err){
            this.trigger("Discovering", false);
          }
        }
      },
      // 'connecting', 'connected', or 'not_connected'
      onConnectionStatusChange: ({status}) => {
        status = status?.toUpperCase();
        if(status === "NOT_CONNECTED") {
          this.connectedReader = null;
          this.readerConnecting = false;
        }
        this.readerConnected = (status === 'CONNECTED');
        this.trigger("ConnectionStatusChange", status);
      },
      onPaymentStatusChange: ({status}) => {
        this.trigger("ReaderStatus", this.PaymentStatusMap[status]);
        this.trigger("PaymentStatusChange", this.PaymentStatusMap[status]);
      },
    });

    if (this.settings.defaultReader?.serial_number) {
      const simulated = this.settings.defaultReader.serial_number.includes("SIMULATOR");
      try {
        this.discoverReaders({
          simulated
        }, (readers) => {
          const foundReader = readers.find(r => r.serial_number === this.settings.defaultReader.serial_number);
          if (foundReader) {
            this.connectReader(foundReader);
          }
        })
      } catch(err){
        this.trigger("Discovering", false);
      }
    }
  }

  setConnectionToken = () => {

  }

  listLocations = () => {

  }

  isDiscovering = async () => {
    return false;
  }

  discoverReaders = async (options = {}, callbackFn = () => {}) => {
    try {
      const result = await this.terminal.discoverReaders(options);
      callbackFn(result.discoveredReaders);
    } catch (err) {
      this.trigger("Discovering", false);
    }
  }

  cancelDiscovery = () => {
    // do nothing
  }

  connectReader = async (reader) => {
    this.readerConnecting = true;
    try {
      let response = await this.terminal.connectReader(reader);
      if (response.error) {
        return response;
      }
      if (response.reader) {
        this.readerConnected = true;
        this.connectedReader = response.reader;
        return response.reader;
      }
    } catch (err) {
      return {
        error: err.toString()
      }
    }
    return reader;
  }

  connectInternetReader = async (reader) => {
    return await this.connectReader(reader);
  }

  connectBluetoothReader = async (serial) => {
    throw new Error("Bluetooth not supported on Web");
  }
  connectUsbReader = async (serial, config) => {
    throw new Error("USB Readers not supported on Web");
  }

  disconnectReader = () => {
    this.connectedReader = null;
    return this.terminal.disconnectReader();
  }

  getConnectedReader = async () => {
    return this.connectedReader;
  }

  getConnectionStatus = () => {
    return this.terminal?.getConnectionStatus();
  }

  get readerStatus(){
    return this?.getConnectionStatus()?.toUpperCase();
  }

  /**
   *
   * @param {object} config - A config object
   * @param {string} config.testCardNumber - The test card number to use
   */
  setSimulatorConfiguration = (config) => {
    this.terminal.setSimulatorConfiguration(config);
  }

  /**
   * Returns the reader’s payment status.
   * PaymentStatus can be one of not_ready, ready, waiting_for_input, or processing.
   * @returns {String}
   */
  getPaymentStatus = () => {
    return this.terminal.getPaymentStatus();
  }

  clearCachedCredentials = () => {
    return this.terminal.clearCachedCredentials();
  }

  createPaymentIntent = async (parameters) => {
    this.cancelling = false;
    const {error, clientSecret} = await this.settings.createPaymentIntent(parameters);
    if(error) return error;

    this._clientSecret = clientSecret;
    return { clientSecret };
  }

  collectPaymentMethod = async () => {
    if (!this._clientSecret) return {error: {message: "Can't collect payment method without calling 'createPaymentIntent' first"}};

    const result = await this.terminal.collectPaymentMethod(this._clientSecret);
    if (result.error) {
      return result;
    } else {
      this._paymentIntent = result.paymentIntent;
    }
    return result;
  }

  retrievePaymentIntent = () => {
    // Not needed by Web Terminal
  }

  cancelCollectPaymentMethod = async () => {
    this.cancelling = true;
    return await this.terminal.cancelCollectPaymentMethod();
  }

  /**
   *
   * @returns {Promise<{paymentIntent: *}|{error: *}>}
   */
  processPayment = async () => {
    const {paymentIntent, error} = await this.terminal.processPayment(this._paymentIntent);
    if(error) return { error };

    this._paymentIntent = paymentIntent;
    return { paymentIntent };
  }

  readReusableCard = async () => {
    this.cancelling = false;
    return await this.terminal.readReusableCard();
  }

  cancelReadReusableCard = () => {
    this.cancelling = true;
    return this.terminal.cancelReadReusableCard();
  }

  /**
   * {
    type: 'cart',
    cart: {
      line_items: [
        {
          description: string,
          amount: number,
          quantity: number,
        },
      ],
      tax: number,
      total: number,
      currency: string,
    }
  }
   * @param displayInfo
   * @returns {*}
   */
  setReaderDisplay = (displayInfo) => this.terminal.setReaderDisplay(displayInfo);

  clearReaderDisplay = () => this.terminal.clearReaderDisplay();

  checkForUpdate = () => {
    // do nothing
  }

  installAvailableUpdate = () => {
    // do nothing
  }

  /**
   * Listen to an event on this model. Returns an object with a 'remove' function
   * which can be called to stop listening to the event.
   * @param event
   * @param fn
   * @returns {{remove: function}}
   */
  on = (event, fn) => {
    if (!this._events[event]) this._events[event] = [fn];
    else this._events[event].push(fn);

    return {remove: () => this.off(event, fn)};
  }

  off = (event, fn) => {
    _.pull(this._events[event], fn);
  }

  trigger = (event, ...args) => {
    if (this._events[event]) {
      this._events[event].forEach(fn => {
        if (typeof fn === 'function') {
          try {
            fn.apply(null, args);
          } catch (err) {
            console.error("Event listener failed to run. ", err);
            this.off(event, fn);
          }
        }
      });
    }
  }

}

const _StripeTerminal = new RNStripeTerminal();

export default _StripeTerminal;
