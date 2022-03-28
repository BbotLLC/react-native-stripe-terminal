/**
 * Use a plus symbol to coerce the ordinal value of an enumerated property:
 * ```
 * +DeviceType.CHIPPER_2X === 0
 * ```
 */

import Enum from './Enum'

export default class DeviceType extends Enum {

  static CHIPPER_2X = new DeviceType('CHIPPER_2X', 'Chipper 2X', 'bbpos_chipper2x', ["CHB20", "CHB22"])
  static STRIPE_M2 = new DeviceType( 'STRIPE_M2', 'Stripe M2', 'stripe_m2', ["CHB30", "CHB31", "STRM2"])
  static COTS_DEVICE = new DeviceType( 'COTS_DEVICE', 'COTS Device', 'mobile_phone_reader', [])
  static VERIFONE_P400 = new DeviceType( 'VERIFONE_P400', 'Verifone P400', 'verifone_P400', [])
  static WISEPAD_3 = new DeviceType( 'WISEPAD_3', 'WisePad 3', 'bbpos_wisepad3', ["WPC30", "WPC32"])
  static WISEPOS_E = new DeviceType("WISEPOS_E", "WisePOS E", "bbpos_wisepos_e", ["WSC51"])
  static UNKNOWN = new DeviceType('UNKNOWN', 'Unknown', "", [])

  #display_name = '';
  #device_name = '';
  #serial_prefixes = [];

  constructor(name, display_name, device_name, serial_prefixes) {
    super(name);
    this.#display_name = display_name;
    this.#device_name = device_name;
    this.#serial_prefixes = serial_prefixes;
  }

  get display_name(){
    return this.#display_name;
  }
  get device_name(){
    return this.#device_name;
  }
  get serial_prefixes(){
    return this.#serial_prefixes;
  }
}
