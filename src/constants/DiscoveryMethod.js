/**
 * Use a plus symbol to coerce the ordinal value of an enumerated property:
 * ```
 * +DiscoverMethod.BLUETOOTH_SCAN === 0
 * ```
 */

import Enum from "./Enum";

export default class DiscoveryMethod extends Enum {

  static BLUETOOTH_SCAN = new DiscoveryMethod('BLUETOOTH_SCAN', 'Bluetooth')
  static INTERNET = new DiscoveryMethod( 'INTERNET', 'Internet')
  static LOCAL_MOBILE = new DiscoveryMethod( 'LOCAL_MOBILE', 'Local Mobile')
  static HANDOFF = new DiscoveryMethod('HANDOFF', 'Handoff')
  static EMBEDDED = new DiscoveryMethod( 'EMBEDDED', 'Embedded')
  static USB = new DiscoveryMethod('USB', 'USB')

  #display_name = '';

  constructor(name, display_name) {
    super(name);
    this.#display_name = display_name;
  }

  get display_name(){
    return this.#display_name;
  }
}
