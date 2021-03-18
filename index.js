import {NativeModules, DeviceEventEmitter} from 'react-native';
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

    let isInitialized = await StripeTerminal.init(settings);

    if (isInitialized) {
      this.configureListeners();

      /*if(settings.defaultReader){
        console.log('attempting to connect to default reader');
        this._lastConnectedReader = settings.defaultReader;
        this.discoverReaders(this.settings, this.autoReconnectReader)
      }*/
    }

    return isInitialized;
  },

  configureListeners() {
    if(_configured) return;
    _configured = true;

    DeviceEventEmitter.addListener("StripeTerminalEvent", data => {

      // not sure if we need any of this...
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
            this.discoverReaders(this.settings, this.autoReconnectReader)
          }
          break;
      }

      this.trigger(data.event, data.data);
    });
  },

  autoReconnectReader(readers){

    if(readers.length > 0){
      let reader = readers.find(r => r.serial === this._lastConnectedReader);
      if(reader){
        this.connectReader(reader.serial);
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
        timeout: 120,
        simulated: false
    }
    if(typeof options !== 'object'){
      options = {
        timeout: options // backwards compatibility
      }
    }
    Object.assign(defaultOptions, options);

    if(options.readerSerial){
      this._lastConnectedReader = options.readerSerial;
    }

    let callback = options.readerSerial ? (readers) => {
      this.autoReconnectReader(readers);
      callbackFn();
    } : callbackFn;

    this._discoverReadersCB = callback;
    return await StripeTerminal.discoverReaders(options);

  },

  async cancelDiscovery() {
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
      let response = await StripeTerminal.connectReader(serial);
      this._lastConnectedReader = serial;
      this.readerConnected = response;
      if (response) {
        this._discoverReadersCB = null;
      }
      return response;
  },


  async disconnectReader() {
    return  await StripeTerminal.disconnectReader();
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
