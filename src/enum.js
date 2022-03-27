class Enum {
  #ordinal;
  #name;

  constructor(name) {
    this.#ordinal = Object.keys(this.constructor).length;
    this.#name = name;
  }

  get ordinal(){
    return this.#ordinal;
  }

  get name(){
    return this.#name;
  }

  static values() {
    return Object.values(this);
  }

  static valueOf(name) {
    return this[name];
  }

  toString(){
    return this.ordinal;
  }
}

/**
 * Use a plus symbol to coerce the ordinal value of an enumerated property:
 * ```
 * +DiscoverMethod.BLUETOOTH_SCAN === 0
 * ```
 */
class DiscoveryMethod extends Enum {

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


