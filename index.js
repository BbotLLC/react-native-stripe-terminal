import {NativeModules, DeviceEventEmitter} from 'react-native';
import _ from "lodash";

// Todo: Handle discoverReader timeout

const {StripeTerminal} = NativeModules;

const events = {};

let _configured = false;

export default {

  readerConnected: false,

  // test to see if Terminal instance is already initialized
  async isInitialized() {
    let res = await StripeTerminal.isInitialized();
    if(res && !_configured) this.configureListeners();

    return res === true;
  },

  async init(settings) {
    let isInitialized = await StripeTerminal.init(settings);

    if (isInitialized) {
      this.configureListeners();
    }

    return isInitialized;
  },

  configureListeners() {
    if(_configured) return;
    _configured = true;
    
    DeviceEventEmitter.addListener("StripeTerminalEvent", data => {
      console.log("Stripe Terminal Event: ");
      console.log(data.event, data.data);

      // not sure if we need any of this...
      switch (data.event) {
        case 'updateDiscoveredReaders':
          if (this._discoverReadersCB) this._discoverReadersCB(data.data);
          break;
        case 'ConnectionStatusChange':
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
          // start scanning?
          // if(settings.autoreconnect)...
          break;
      }

      this.trigger(data.event, data.data);
    });
  },

  async getConnectedReader() {
    let reader = await StripeTerminal.getConnectedReader();
    this.readerConnected = !!reader;
    this.trigger('ConnectionStatusChange', reader ? "CONNECTED" : "NOT_CONNECTED");
    return reader;
  },

  /**
   *
   * @param timeout
   * @param callbackFn - The callbackFn is passed the readers array everytime we poll for new readers
   * @returns {Promise<*>}
   */
  async discoverReaders(timeout, callbackFn) {

    this._discoverReadersCB = callbackFn;
    return await StripeTerminal.discoverReaders(timeout);

  },

  async cancelDiscovery(){
    return await StripeTerminal.cancelDiscovery();
  },

  async destroyListeners() {
    DeviceEventEmitter.removeAllListeners();
  },

  async connectReader(serial) {
    this.cancelDiscovery();

    let response = await StripeTerminal.connectReader(serial);
    this.readerConnected = response;
    if (response === true) {
      this._discoverReadersCB = null;
    }
    return response;
  },


  async disconnectReader() {
    return  await StripeTerminal.disconnectReader();
  },

  async createPaymentIntent(amount, currency) {
    if (!currency) currency = "usd";

    return await StripeTerminal.createPaymentIntent(amount, currency);
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

  on(event, fn) {
    if (!events[event]) events[event] = [fn];
    else events[event].push(fn);
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
            console.error("Event listener failed to run. ", err);
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