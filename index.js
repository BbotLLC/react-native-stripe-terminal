import {NativeModules, DeviceEventEmitter, PermissionsAndroid, NativeEventEmitter} from 'react-native';
import _ from "lodash";

// Todo: Handle discoverReader timeout

const {StripeTerminal} = NativeModules;

const events = {};
const constants = StripeTerminal?.getConstants() || {DiscoveryMethods: []};

let _configured = false;

class RNStripeTerminal {

  DiscoveryMethods = constants.DiscoveryMethods.reduce( (dict, val, i) => { dict[val] = i; return dict}, {});

  settings = {
    fetchConnectionToken: () => {},
    scanTimeout: 120,
    simulated: false,
    autoReconnect: true,
    defaultReader: null
  };

  _lastConnectedReader = null;

  readerConnected = false;
  readerStatus = null;

  _fetchConnectionToken = () => {
    throw new Error("fetchConnectionToken is required");
  }

  constructor() {
    this.configureListener();
  }

  // test to see if Terminal instance is already initialized
  async isInitialized() {
    let res = await StripeTerminal.isInitialized();
    if (res && !_configured) this.configureListener();

    return res === true;
  };

  init = async (settings) => {
    this.settings = Object.assign({}, this.settings, settings);
    this._fetchConnectionToken = this.settings.fetchConnectionToken;

    let allowed = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
    if (!allowed) {
      throw new Error("Location permission required");
    }

    let isInitialized = await StripeTerminal.init(settings);

    if (isInitialized) {

      if (settings.defaultReader) {
        this._lastConnectedReader = settings.defaultReader;
        this._autoReconnectListener = this.on('UpdateDiscoveredReaders', this.autoReconnectReader.bind(this));

        this.discoverReaders({
            discoveryMethod: this.settings.discoveryMethod || this.DiscoveryMethods.BLUETOOTH_SCAN,
            simulated: this.settings.simulated,
            timeout: 120
        }, () => {
          console.log('auto discover, looking for ', settings.defaultReader);
        }).then(() => {
          this.trigger('discoverFinished', this.readerConnected);
        });
      }
    }

    return isInitialized;
  };

  configureListener() {
    if (_configured) return;
    _configured = true;

    const emitter = new NativeEventEmitter(StripeTerminal);

    this.listener = emitter.addListener("StripeTerminalEvent", data => {
      console.log("StripeTerminalJS got event: ", data.event, data.data);

      // UnexpectedDisconnect doesn't get called it appears
      switch (data.event) {
        case 'RequestConnectionToken':
            console.log('fetching connection token...');
          this._fetchConnectionToken()
          .then(token => {
            console.log('got token: ', token);
            if(token) StripeTerminal.setConnectionToken(token, null);
            else throw new Error("No token");
          }).catch(err => {
              console.log('error fetching token: ', err);
            StripeTerminal.setConnectionToken(null, err.message || "Error fetching connection token");
          });
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
      let reader = readers.find(r => r.serial === this._lastConnectedReader);
      if (reader) {
        // TODO: use proper connection function (connectBluetooth vs connectInternet)
        await this.connectBluetoothReader(reader.serial);
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

    console.log('Calling discoverReaders: ', options);
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
  async connectInternetReader(serial) {
    let response = await StripeTerminal.connectInternetReader(serial);
    this._lastConnectedReader = serial;
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
  async connectBluetoothReader(serial, config = {}) {
    let response = await StripeTerminal.connectBluetoothReader(serial, config);
    this._lastConnectedReader = serial;
    this.readerConnected = response;
    if (response) {
      this._discoverReadersCB = null;
    }

    return response;
  };


  async disconnectReader() {
    return await StripeTerminal.disconnectReader();
  };

  async createPaymentIntent(amount, currency, statementDescriptor) {
    if (!currency) currency = "usd";

    return await StripeTerminal.createPaymentIntent(amount, currency, statementDescriptor);
  };

  async collectPaymentMethod() {
    return await StripeTerminal.collectPaymentMethod();
  };

  async setPaymentIntent(clientSecret) {
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
