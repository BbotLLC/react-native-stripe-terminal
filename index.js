import {NativeModules, DeviceEventEmitter, PermissionsAndroid} from 'react-native';
import _ from "lodash";

// Todo: Handle discoverReader timeout

const {StripeTerminal} = NativeModules;

const events = {};

let _configured = false;

export default {

  settings: {
    url: '',
    authToken: '',
    scanTimeout: 120,
    simulated: false,
    autoReconnect: true,
    defaultReader: null
  },

  _lastConnectedReader: null,

  readerConnected: false,
  readerStatus: null,

  // test to see if Terminal instance is already initialized
  async isInitialized() {
    let res = await StripeTerminal.isInitialized();
    if(res && !_configured) this.configureListeners();

    return res === true;
  },

  async init(settings) {
    this.settings = settings;

    let allowed = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
    if(!allowed){
      let result = await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
      if(result === PermissionsAndroid.RESULTS.GRANTED){
        allowed = true;
      }
    }

    if(!allowed) {
      throw new Error("Location permission required");
    }

    let isInitialized = await StripeTerminal.init(settings);

    if (isInitialized) {
      this.configureListeners();

      if(settings.defaultReader){
        this._lastConnectedReader = settings.defaultReader;
        this.on('updateDiscoveredReaders', this.autoReconnectReader.bind(this));
        this.discoverReaders({simulated: this.settings.simulated}).then(() => {

          this.trigger('discoverFinished', this.readerConnected);
        });
      }
    }

    return isInitialized;
  },

  configureListeners() {
    if(_configured) return;
    _configured = true;

    DeviceEventEmitter.addListener("StripeTerminalEvent", data => {
      // UnexpectedDisconnect doesn't get called it appears
      switch (data.event) {
        case 'updateDiscoveredReaders':
          if (this._discoverReadersCB) this._discoverReadersCB(data.data);
          break;
        case 'updateProgress':
          if(this._progressCallback) this._progressCallback(data.data);
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
          if (this.settings.autoReconnect && this._lastConnectedReader){
            this.discoverReaders(this.settings);
          }
          break;
      }

      this.trigger(data.event, data.data);
    });
  },

  async autoReconnectReader(readers){
    if(readers.length > 0){
      let reader = readers.find(r => r.serial === this._lastConnectedReader);
      if(reader){
        await this.connectReader(reader.serial);
      }
    }
  },

  async getConnectedReader() {
    let reader = await StripeTerminal.getConnectedReader();
    this.readerConnected = !!reader;
    this.readerStatus = reader ? "CONNECTED" : "NOT_CONNECTED";
    this.trigger('ConnectionStatusChange', this.readerStatus);
    return reader;
  },

  /**
   *
   * @param options
   * @param callbackFn - The callbackFn is passed the readers array everytime we poll for new readers
   * @returns {Promise<*>}
   */
  async discoverReaders(options, callbackFn = ()=>{}) {
    let defaultOptions = {
        timeout: this.settings.scanTimeout,
        simulated: this.settings.simulated
    }
    if(typeof options !== 'object'){
      options = {
        timeout: options // backwards compatibility
      }
    }
    options = Object.assign(defaultOptions, options);
    this._discoverReadersCB = callbackFn;
    this._isDiscovering = true;
    return await StripeTerminal.discoverReaders(options);
  },

  async cancelDiscovery() {
    this._isDiscovering = false;
    return await StripeTerminal.cancelDiscovery();
  },

  async destroyListeners() {
    DeviceEventEmitter.removeAllListeners();
  },

  /**
   * Throws error if unsuccessful, be sure to call within try/catch block
   * @param serial
   * @returns {Promise<*>}
   */
  async connectReader(serial) {
    this._isDiscovering = false;
    let response = await StripeTerminal.connectReader(serial);
    this._lastConnectedReader = serial;
    this.readerConnected = response;
    if (response) {
      this._discoverReadersCB = null;
    }
    return response;
  },


  async disconnectReader() {
    return await StripeTerminal.disconnectReader();
  },

  async createPaymentIntent(amount, currency, statementDescriptor) {
    if (!currency) currency = "usd";

    return await StripeTerminal.createPaymentIntent(amount, currency, statementDescriptor);
  },

  async collectPaymentMethod() {
    return await StripeTerminal.collectPaymentMethod();
  },

  async cancelCollectPaymentMethod(){
    return await StripeTerminal.cancelCollectPaymentMethod();
  },

  async confirmPaymentIntent() {
    return await StripeTerminal.confirmPaymentIntent();
  },

  async readReusableCard(){
    return await StripeTerminal.readReusableCard();
  },

  async checkForUpdate(){
    this._progressCallback = null;
    return await StripeTerminal.checkForUpdate();
  },

  async installUpdate(progressCallback){
    this._progressCallback = progressCallback;

    return await StripeTerminal.installUpdate();
  },

  on(event, fn) {
    let self = this;
    if (!events[event]) events[event] = [fn];
    else events[event].push(fn);

    return {
      event: event,
      fn: fn,
      remove() { self.off(event, fn)}
    };
  },

  off(event, fn) {
    _.pull(events[event], fn);
  },

  trigger(event, ...args) {
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
  },

  onEvent(callbackFn) {
    this._allEventsCallback = callbackFn;
  }


}
