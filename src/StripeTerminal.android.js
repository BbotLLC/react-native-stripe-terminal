import {NativeModules, DeviceEventEmitter, PermissionsAndroid, NativeEventEmitter} from 'react-native';
import _ from "lodash";

// Todo: Handle discoverReader timeout

const {StripeTerminal} = NativeModules;

const events = {};
const constants = StripeTerminal?.getConstants() || {DiscoveryMethods: []};

let _configured = false;

class RNStripeTerminal {

  DiscoveryMethods = constants.DiscoveryMethods.reduce((dict, entry) => ({...dict, [entry.name]: entry.ordinal}), {})

  AvailableMethods = ['INTERNET', 'BLUETOOTH_SCAN', 'USB'];

  DeviceTypes = {
    BLUETOOTH_SCAN: ['WISEPAD_3', 'STRIPE_M2', 'CHIPPER_2X'],
    INTERNET: ['VERIFONE_P400', 'WISEPOS_E']
  }

  settings = {
    fetchConnectionToken: () => { throw new Error("fetchConnectionToken is required"); },
    /**
     * createPaymentIntent should return a dict with `clientSecret` as the key
     */
    createPaymentIntent: () => { throw "createPaymentIntent is required" }, // used by internet terminals
    scanTimeout: 120,
    simulated: false,
    autoReconnect: true,
    defaultReader: null,
    locationId: null
  };

  _lastConnectedReader = null;

  connectedReader = null;
  readerConnected = false;
  readerStatus = null;

  constructor() {
    this.configureListener();
  }

  getDiscoveryMethodFromDeviceType(type) {
    for(let key in this.DeviceTypes){
      if(this.DeviceTypes[key].includes(type)) return this.DiscoveryMethods[key];
    }
    return 0;
  }

  // test to see if Terminal instance is already initialized
  async isInitialized() {
    let res = await StripeTerminal.isInitialized();
    if (res && !_configured) this.configureListener();

    return res === true;
  };

  init = async (settings) => {
    this.settings = Object.assign({}, this.settings, settings);

    if(await StripeTerminal.isInitialized()){
      return true;
    }

    let allowed = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
    if (!allowed) {
      throw new Error("Location permission required");
    }

    this.configureListener();

    let isInitialized = await StripeTerminal.isInitialized();
    if (!isInitialized) {
      isInitialized = await StripeTerminal.init(settings);

      if (settings.defaultReader) {
        this._lastConnectedReader = settings.defaultReader;
        this._autoReconnectListener = this.on('UpdateDiscoveredReaders', this.autoReconnectReader.bind(this));
        const simulated = this.settings.defaultReader?.serial_number?.includes("SIMULATOR");
        let discoveryMethod = this.getDiscoveryMethodFromDeviceType(this.settings.defaultReader.device_type);
        if(settings.defaultReader.discovery_method) {
          discoveryMethod = this.DiscoveryMethods[settings.defaultReader.discovery_method];
        }

        try {
          this.discoverReaders({
            discoveryMethod,
            simulated,
            timeout: 120
          }, () => {

          }).then(() => {
            this.trigger('DiscoverFinished', this.readerConnected);
          });
        } catch(err){
          this.trigger("Discovering", false);
        }
      }
    }

    return isInitialized;
  };

  configureListener() {
    if (_configured) return;
    _configured = true;

    const emitter = new NativeEventEmitter(StripeTerminal);

    this.listener = emitter.addListener("StripeTerminalEvent", async (data) => {
      // UnexpectedDisconnect doesn't get called it appears
      switch (data.event) {
        case 'RequestConnectionToken':
          try {
            let token = await this.settings.fetchConnectionToken();
            if (token) StripeTerminal.setConnectionToken(token, null);
            else throw new Error("No token");
          } catch(err) {
            console.log('error fetching token: ', err);
            StripeTerminal.setConnectionToken(null, err.message || "Error fetching connection token");
          }
          break;
        case 'UpdateDiscoveredReaders':
          if (this._discoverReadersCB) this._discoverReadersCB(data.data);
          break;
        case 'StartInstallingUpdate':
          break;
        case 'ReaderSoftwareUpdateProgress':
          if (this._progressCallback) this._progressCallback(data.data);
          break;
        case 'UpdateAvailable':
          break;
        case 'ConnectionStatusChange':
          this.readerStatus = data.data;
          switch (data.data) {
            case 'CONNECTING':
              this.readerConnecting = true;
              break;
            case 'CONNECTED':
              this.readerConnected = true;
              this.readerConnecting = false;
              break;
            case 'NOT_CONNECTED':
              this.readerConnected = false;
              this.readerConnecting = false;
              this.connectedReader = null;
              break;
          }
          break;
        case 'BatteryLevelUpdate':
          // batteryLevel
          // batteryStatus
          // isCharging
          break;
        case 'UnexpectedDisconnect':
          if (this.settings.autoReconnect && this._lastConnectedReader) {
            try {
              this.discoverReaders(this.settings);
            } catch(err){
              this.trigger("Discovering", false);
            }
          }
          break;
      }

      this.trigger(data.event, data.data);
    });
  };

  async autoReconnectReader(readers) {
    if (readers.length > 0) {
      let reader = readers.find(r => r.serial_number === this._lastConnectedReader.serial_number);
      if (reader) {
        let discoveryMethod = this._lastConnectedReader?.discovery_method;
        if(discoveryMethod)
          discoveryMethod = this.DiscoveryMethods[discoveryMethod];
        else
          discoveryMethod = this.getDiscoveryMethodFromDeviceType(reader.device_type);

        switch (discoveryMethod){
          case this.DiscoveryMethods.BLUETOOTH_SCAN:
            await this.connectBluetoothReader(reader, { locationId: this.settings.locationId || reader.locationId });
            break;
          case this.DiscoveryMethods.INTERNET:
            await this.connectInternetReader(reader);
            break;
          case this.DiscoveryMethods.USB:
            await this.connectUsbReader(reader);
            break;
        }

        this._autoReconnectListener.remove();
      }
    }
  };

  async getConnectedReader() {
    let reader = await StripeTerminal.getConnectedReader();
    this.readerConnected = !!reader;
    this.connectedReader = reader;
    this.readerStatus = reader ? "CONNECTED" : "NOT_CONNECTED";
    //this.trigger('ConnectionStatusChange', this.readerStatus);
    return reader;
  };

  async listLocations(options = {}) {
    return await StripeTerminal.listLocations(options);
  };

  async isDiscovering() {
    return await StripeTerminal.isDiscovering();
  }

  setDiscoverCallback(callbackFn) {
    this._discoverReadersCB = callbackFn;
  }

  /**
   *
   * @param options
   * @param callbackFn - The callbackFn is passed the readers array everytime we poll for new readers
   * @returns {Promise<*>}
   */
  async discoverReaders(options, callbackFn = () => {
  }) {
    if (await this.isDiscovering()) return;

    let defaultOptions = {
      timeout: this.settings.scanTimeout,
      simulated: this.settings.simulated,
      discoveryMethod: this.DiscoveryMethods.BLUETOOTH_SCAN
    }
    options = Object.assign({}, defaultOptions, options);
    this._discoverReadersCB = callbackFn;

    return await StripeTerminal.discoverReaders(options);
  };


  async cancelDiscovery() {
    return await StripeTerminal.cancelDiscovery();
  };

  async destroyListeners() {
    DeviceEventEmitter.removeAllListeners();
  };

  /**
   * Throws error if unsuccessful, be sure to call within try/catch block
   * @param serial
   * @returns {Promise<*>}
   */
  async connectInternetReader(reader) {
    this.readerConnecting = true;
    let response = await StripeTerminal.connectInternetReader(reader.serial_number);
    response.discovery_method = "INTERNET";
    this._lastConnectedReader = response;
    this.connectedReader = response;
    this.readerConnected = response;

    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  };

  /**
   * Throws error if unsuccessful, be sure to call within try/catch block
   * @param serial
   * @param config {Object} The configuration object
   * @param config.locationId {String} Stripe Terminal Location Id to connect the reader to
   * @returns {Promise<*>}
   */
  async connectBluetoothReader(reader, config = {}) {
    this.readerConnecting = true;
    let response = await StripeTerminal.connectBluetoothReader(reader.serial_number, config);
    response.discovery_method = "BLUETOOTH_SCAN";
    this._lastConnectedReader = response;
    this.readerConnected = response;
    this.connectedReader = response;
    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  };

  async connectUsbReader(reader, config = {}) {
    this.readerConnecting = true;
    let response = await StripeTerminal.connectUsbReader(reader.serial_number, config);
    response.discovery_method = "USB";
    this._lastConnectedReader = response;
    this.readerConnected = response;
    this.connectedReader = response;

    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  }

  async connectEmbeddedReader(reader) {
    this.readerConnecting = true;
    let response = await StripeTerminal.connectEmbeddedReader(reader.serial_number);
    response.discovery_method = "EMBEDDED";
    this._lastConnectedReader = response;
    this.readerConnected = response;
    this.connectedReader = response;
    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  }


  async disconnectReader() {
    this.connectedReader = null;
    return await StripeTerminal.disconnectReader();
  };

   /**
   * @param {object} config - A config object
   * @param {string} config.testCardNumber - The test card number to use
   */
  setSimulatorConfiguration = (config) => {
    StripeTerminal.setSimulatorConfiguration(config);
  }

  /**
   *
   * @param parameters {object} Parameters to pass to `createPaymentIntent` call
   * @param forceRemote {boolean} Always use the `createPaymentIntent` function passed in on init
   * @returns {Promise<*>}
   */
  async createPaymentIntent(parameters = {}, forceRemote = true) {
    this.cancelling = false;
    if (!parameters.currency) parameters.currency = "usd";
    const reader = await this.getConnectedReader();

    if (forceRemote || this.DeviceTypes.INTERNET.includes(reader.device_type)) {
      // Use the server-side 'createPaymentIntent' endpoint:
      const {error, clientSecret} = await this.settings.createPaymentIntent(parameters);
      if(error) return error;

      return await StripeTerminal.retrievePaymentIntent(clientSecret);
    } else {
      // Use the native 'createPaymentIntent' method:
      return await StripeTerminal.createPaymentIntent(parameters);
    }
  };

  async collectPaymentMethod() {
    return await StripeTerminal.collectPaymentMethod();
  };

  async retrievePaymentIntent(clientSecret) {
    return await StripeTerminal.retrievePaymentIntent(clientSecret);
  }

  async cancelCollectPaymentMethod() {
    this.cancelling = true;
    return await StripeTerminal.cancelCollectPaymentMethod();
  };

  async processPayment() {
    return await StripeTerminal.processPayment();
  };

  async readReusableCard() {
    this.cancelling = false;
    return await StripeTerminal.readReusableCard();
  };

  async cancelReadReusableCard() {
    this.cancelling = true;
    return await StripeTerminal.cancelReadReusableCard();
  };

  async checkForUpdate() {
    this._progressCallback = null;
    return await StripeTerminal.checkForUpdate();
  };

  async installAvailableUpdate(callback) {
    this._progressCallback = callback;
    return await StripeTerminal.installAvailableUpdate();
  };

  on = (event, fn) => {
    if (!events[event]) events[event] = [fn];
    else events[event].push(fn);

    return {
      event,
      fn,
      remove: () => {
        this.off(event, fn)
      }
    };
  };

  off = (event, fn) => {
    _.pull(events[event], fn);
  };

  trigger = (event, ...args) => {
    if (this._allEventsCallback) {
      this._allEventsCallback(event, ...args);
    }

    if (events[event]) {
      events[event].forEach(fn => {
        if (typeof fn === 'function') {
          try {
            fn.apply(null, args);
          } catch (err) {
            console.error("Stripe Event listener failed to run. ", err);
            this.off(event, fn);
          }
        }
      });
    }
  };

  onEvent(callbackFn) {
    this._allEventsCallback = callbackFn;
  }

  // Call when you unmount
  cleanup = () => {
    this.listener.remove();
  }

}

const _StripeTerminal = new RNStripeTerminal();

export default _StripeTerminal;
