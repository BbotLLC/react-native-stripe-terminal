import {NativeModules, DeviceEventEmitter, PermissionsAndroid, NativeEventEmitter} from 'react-native';
import _ from "lodash";

// Todo: Handle discoverReader timeout

const {StripeTerminal} = NativeModules;

const events = {};
const constants = StripeTerminal?.getConstants() || {DiscoveryMethods: []};

let _configured = false;

class RNStripeTerminal {

  DiscoveryMethods = constants.DiscoveryMethods.reduce((dict, entry) => ({...dict, [entry.ordinal]: entry.name}), {})

  AvailableMethods = ['INTERNET', 'BLUETOOTH'];

  DeviceTypes = {
    BLUETOOTH_SCAN: ['WISEPAD_3', 'STRIPE_M2', 'CHIPPER_2X'],
    INTERNET: ['VERIFONE_P400', 'WISEPOS_E']
  }

  settings = {
    fetchConnectionToken: () => { throw new Error("fetchConnectionToken is required"); },
    createPaymentIntent: () => { throw "createPaymentIntent is required" }, // used by internet terminals
    scanTimeout: 120,
    simulated: false,
    autoReconnect: true,
    defaultReader: null
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
    return null;
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
        const simulated = this.settings.defaultReader.serial_number.includes("SIMULATOR");
        const discoveryMethod = this.getDiscoveryMethodFromDeviceType(this.settings.defaultReader.device_type);
        this.discoverReaders({
          discoveryMethod,
          simulated,
          timeout: 120
        }, () => {
          console.log('auto discover, looking for ', settings.defaultReader);
        }).then(() => {
          this.trigger('DiscoverFinished', this.readerConnected);
        });
      }
    }

    return isInitialized;
  };

  configureListener() {
    if (_configured) return;
    _configured = true;

    const emitter = new NativeEventEmitter(StripeTerminal);

    this.listener = emitter.addListener("StripeTerminalEvent", async (data) => {
      console.log("StripeTerminalJS got event: ", data.event, data.data);

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
          console.log('Starting to install update');

          break;
        case 'ReaderSoftwareUpdateProgress':
          if (this._progressCallback) this._progressCallback(data.data);
          break;
        case 'UpdateAvailable':
          console.log("Reader Update is Available");
          break;

        case 'ConnectionStatusChange':
          this.readerStatus = data.data;
          switch (data.data) {
            case 'CONNECTING':
              this.readerConnecting = true;
              break;
            case 'CONNECTED':
              this.readerConnected = true;
              break;
            case 'NOT_CONNECTED':
              this.readerConnected = false;
              break;
          }
          break;
        case 'UnexpectedDisconnect':
          if (this.settings.autoReconnect && this._lastConnectedReader) {
            this.discoverReaders(this.settings);
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
        const discoveryMethod = this.getDiscoveryMethodFromDeviceType(reader.device_type);
        switch (discoveryMethod){
          case this.DiscoveryMethods.BLUETOOTH_SCAN:
            await this.connectBluetoothReader(reader);
            break;
          case this.DiscoveryMethods.INTERNET:
            await this.connectInternetReader(reader);
            break;
        }

        this._autoReconnectListener.remove();
      }
    }
  };

  async getConnectedReader() {
    let reader = await StripeTerminal.getConnectedReader();
    this.readerConnected = !!reader;
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
    let response = await StripeTerminal.connectInternetReader(reader.serial_number);
    this._lastConnectedReader = reader;
    this.readerConnected = response;
    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  };

  /**
   * Throws error if unsuccessful, be sure to call within try/catch block
   * @param serial
   * @param config
   * @returns {Promise<*>}
   */
  async connectBluetoothReader(reader, config = {}) {
    let response = await StripeTerminal.connectBluetoothReader(reader.serial_number, config);
    this._lastConnectedReader = reader;
    this.readerConnected = response;
    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  };

  async connectEmbeddedReader(reader) {
    let response = await StripeTerminal.connectEmbeddedReader(reader.serial_number);
    this._lastConnectedReader = reader;
    this.readerConnected = response;
    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  }


  async disconnectReader() {
    return await StripeTerminal.disconnectReader();
  };

  async createPaymentIntent(parameters = {}) {
    if (!parameters.currency) parameters.currency = "usd";
    const reader = await this.getConnectedReader();

    if (reader.internet_reader) {
      const clientSecret = await this.settings.createPaymentIntent(parameters);
      return await StripeTerminal.retrievePaymentIntent(clientSecret);
    } else {
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
    return await StripeTerminal.cancelCollectPaymentMethod();
  };

  async confirmPaymentIntent() {
    return await StripeTerminal.confirmPaymentIntent();
  };

  async readReusableCard() {
    return await StripeTerminal.readReusableCard();
  };

  async cancelReadReusableCard() {
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