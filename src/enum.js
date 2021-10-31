class Enum {
  static values() {
    return Object.keys(this).map(key => this[key]);
  }
}

Enum.prototype.valueOf = function () {
  return this.ordinal;
}

/**
 * Use a plus symbol to coerce the ordinal value of a enumerated property:
 * +DiscoverMethod.BLUETOOTH_SCAN === 0
 */
class DiscoveryMethod extends Enum {

  static BLUETOOTH_SCAN = new DiscoveryMethod(0, 'BLUETOOTH_SCAN', 'Bluetooth');
  static INTERNET = new DiscoveryMethod(1, 'INTERNET', 'Internet');
  static LOCAL_MOBILE = new DiscoveryMethod(2, 'LOCAL_MOBILE', 'Local Mobile');
  static HANDOFF = new DiscoveryMethod(3, 'HANDOFF', 'Handoff');
  static EMBEDDED = new DiscoveryMethod(4, 'EMBEDDED', 'Embedded');

  ordinal = 0;
  name = '';
  display_name = '';

  constructor(ordinal, name, display_name) {
    super();
    this.ordinal = ordinal;
    this.name = name;
    this.display_name = display_name;
  }
}


