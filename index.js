import {NativeModules, DeviceEventEmitter, PermissionsAndroid} from 'react-native';
import _ from "lodash";

// Todo: Handle discoverReader timeout

const {StripeTerminal} = NativeModules;

const events = {};
const constants = StripeTerminal?.getConstants();

let _configured = false;

export default class RNSTripeTerminal {

  static settings = {
    url: '',
    authToken: '',
    scanTimeout: 120,
    simulated: false,
    autoReconnect: true,
    defaultReader: null
  };

  static _lastConnectedReader = null;

  static readerConnected = false;
  static readerStatus = null;

  // test to see if Terminal instance is already initialized
  static async isInitialized() {
    let res = await StripeTerminal.isInitialized();
    if (res && !_configured) RNSTripeTerminal.configureListener();

    return res === true;
  };

  static async init(settings) {
    RNSTripeTerminal.settings = settings;

    let allowed = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
    if (!allowed) {
      throw new Error("Location permission required");
    }

    let isInitialized = await StripeTerminal.init(settings);

    if (isInitialized) {
      await RNSTripeTerminal.configureListener();

      if (settings.defaultReader) {
        RNSTripeTerminal._lastConnectedReader = settings.defaultReader;
        RNSTripeTerminal.on('updateDiscoveredReaders', RNSTripeTerminal.autoReconnectReader.bind(this));

        RNSTripeTerminal.discoverReaders({simulated: RNSTripeTerminal.settings.simulated}, () => {
          console.log('auto discover, looking for ', settings.defaultReader);
        }).then(() => {
          RNSTripeTerminal.trigger('discoverFinished', RNSTripeTerminal.readerConnected);
        });
      }

    }

    return isInitialized;
  };

  static configureListener() {
    if (_configured) return;
    _configured = true;

    DeviceEventEmitter.addListener("StripeTerminalEvent", data => {
      console.log(data.event, data.data);
      // UnexpectedDisconnect doesn't get called it appears
      switch (data.event) {
        case 'updateDiscoveredReaders':
          if (RNSTripeTerminal._discoverReadersCB) RNSTripeTerminal._discoverReadersCB(data.data);
          break;
        case 'StartInstallingUpdate':
          console.log('Starting to install update');

          break;
        case 'ReaderSoftwareUpdateProgress':
          if (RNSTripeTerminal._progressCallback) RNSTripeTerminal._progressCallback(data.data);
          break;
        case 'UpdateAvailable':
          console.log("Reader Update is Available");
          break;

        case 'ConnectionStatusChange':
          RNSTripeTerminal.readerStatus = data.data;
          switch (data.data) {
            case 'CONNECTING':
              RNSTripeTerminal.readerConnecting = true;
              break;
            case 'CONNECTED':
              RNSTripeTerminal.readerConnected = true;
              break;
            case 'NOT_CONNECTED':
              RNSTripeTerminal.readerConnected = false;
              break;
          }
          break;
        case 'UnexpectedDisconnect':
          if (RNSTripeTerminal.settings.autoReconnect && RNSTripeTerminal._lastConnectedReader) {
            RNSTripeTerminal.discoverReaders(RNSTripeTerminal.settings);
          }
          break;
      }

      RNSTripeTerminal.trigger(data.event, data.data);
    });
  };

  static async autoReconnectReader(readers) {
    if (readers.length > 0) {
      let reader = readers.find(r => r.serial === RNSTripeTerminal._lastConnectedReader);
      if (reader) {
        // TODO: use proper connection function (connectBluetooth vs connectInternet)
        await RNSTripeTerminal.connectBluetoothReader(reader.serial);
      }
    }
  };

  static async getConnectedReader() {
    let reader = await StripeTerminal.getConnectedReader();
    RNSTripeTerminal.readerConnected = !!reader;
    RNSTripeTerminal.readerStatus = reader ? "CONNECTED" : "NOT_CONNECTED";
    //RNSTripeTerminal.trigger('ConnectionStatusChange', RNSTripeTerminal.readerStatus);
    return reader;
  };

  static async listLocations(options = {}) {
    return await StripeTerminal.listLocations(options);
  };

  static async isDiscovering() {
    return await StripeTerminal.isDiscovering();
  }

  /**
   *
   * @param options
   * @param callbackFn - The callbackFn is passed the readers array everytime we poll for new readers
   * @returns {Promise<*>}
   */
  static async discoverReaders(options, callbackFn = () => {}) {
    if(await RNSTripeTerminal.isDiscovering()) return;

    let defaultOptions = {
      timeout: RNSTripeTerminal.settings.scanTimeout,
      simulated: RNSTripeTerminal.settings.simulated
    }
    if (typeof options !== 'object') {
      options = {
        timeout: options // backwards compatibility
      }
    }
    options = Object.assign(defaultOptions, options);
    RNSTripeTerminal._discoverReadersCB = callbackFn;

    return await StripeTerminal.discoverReaders(options);
  };


  static async cancelDiscovery() {
    return await StripeTerminal.cancelDiscovery();
  };

  static async destroyListeners() {
    DeviceEventEmitter.removeAllListeners();
  };

  /**
   * Throws error if unsuccessful, be sure to call within try/catch block
   * @param serial
   * @returns {Promise<*>}
   */
  static async connectInternetReader(serial) {
    let response = await StripeTerminal.connectInternetReader(serial);
    RNSTripeTerminal._lastConnectedReader = serial;
    RNSTripeTerminal.readerConnected = response;
    if (response) {
      RNSTripeTerminal._discoverReadersCB = null;
    }

    return response;
  };

  /**
   * Throws error if unsuccessful, be sure to call within try/catch block
   * @param serial
   * @param config
   * @returns {Promise<*>}
   */
  static async connectBluetoothReader(serial, config = {}) {
    let response = await StripeTerminal.connectBluetoothReader(serial, config);
    RNSTripeTerminal._lastConnectedReader = serial;
    RNSTripeTerminal.readerConnected = response;
    if (response) {
      RNSTripeTerminal._discoverReadersCB = null;
    }

    return response;
  };


  static async disconnectReader() {
    return await StripeTerminal.disconnectReader();
  };

  static async createPaymentIntent(amount, currency, statementDescriptor) {
    if (!currency) currency = "usd";

    return await StripeTerminal.createPaymentIntent(amount, currency, statementDescriptor);
  };

  static async collectPaymentMethod() {
    return await StripeTerminal.collectPaymentMethod();
  };

  static async cancelCollectPaymentMethod() {
    return await StripeTerminal.cancelCollectPaymentMethod();
  };

  static async confirmPaymentIntent() {
    return await StripeTerminal.confirmPaymentIntent();
  };

  static async readReusableCard() {
    return await StripeTerminal.readReusableCard();
  };

  static async cancelReadReusableCard() {
    return await StripeTerminal.cancelReadReusableCard();
  };

  static async checkForUpdate() {
    RNSTripeTerminal._progressCallback = null;
    return await StripeTerminal.checkForUpdate();
  };

  static async installAvailableUpdate(callback) {
    RNSTripeTerminal._progressCallback = callback;
    return await StripeTerminal.installAvailableUpdate();
  };

  static on(event, fn) {
    if (!events[event]) events[event] = [fn];
    else events[event].push(fn);

    return {
      event: event,
      fn: fn,
      remove() {
        RNSTripeTerminal.off(event, fn)
      }
    };
  };

  static off(event, fn) {
    _.pull(events[event], fn);
  };

  static trigger(event, ...args) {
    if (RNSTripeTerminal._allEventsCallback) {
      RNSTripeTerminal._allEventsCallback(event, ...args);
    }

    if (events[event]) {
      events[event].forEach(fn => {
        if (typeof fn === 'function') {
          try {
            fn.apply(null, args);
          } catch (err) {
            console.error("Stripe Event listener failed to run. ", err);
            RNSTripeTerminal.off(event, fn);
          }
        }
      });
    }
  };

  static onEvent(callbackFn) {
    RNSTripeTerminal._allEventsCallback = callbackFn;
  }


}
