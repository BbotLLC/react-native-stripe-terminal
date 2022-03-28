/**
 * Default Constants which get overridden by Android if available.
 */

export default {
  "DeviceTypes": [
    {
      "device_name": "bbpos_chipper2x",
      "name": "CHIPPER_2X",
      "display_name": "Chipper 2X",
      "ordinal": 0,
      "serial_prefixes": [ "CHB20", "CHB22"],
    },
    {
      "device_name": "stripe_m2",
      "name": "STRIPE_M2",
      "display_name": "Stripe M2",
      "ordinal": 1,
      "serial_prefixes": ["CHB30", "CHB31", "STRM2"]
    },
    {
      "device_name": "mobile_phone_reader",
      "name": "COTS_DEVICE",
      "display_name": "COTS Device",
      "ordinal": 2,
      "serial_prefixes": []
    },
    {
      "device_name": "verifone_P400",
      "name": "VERIFONE_P400",
      "display_name": "Verifone P400",
      "ordinal": 3,
      "serial_prefixes": []
    },
    {
      "device_name": "bbpos_wisepad3",
      "name": "WISEPAD_3",
      "display_name": "WisePad 3",
      "ordinal": 4,
      "serial_prefixes": ["WPC30", "WPC32"]
    },
    {
      "device_name": "bbpos_wisepos_e",
      "name": "WISEPOS_E",
      "display_name": "WisePOS E",
      "ordinal": 5,
      "serial_prefixes": ["WSC51"]
    },
    {
      "device_name": "",
      "name": "UNKNOWN",
      "display_name": "Unknown",
      "ordinal": 6,
      "serial_prefixes": []
    }
  ],
  "DiscoveryMethods": [
    {
      "ordinal": 0,
      "name": "BLUETOOTH_SCAN",
      "devices": ["CHIPPER_2X", "STRIPE_M2", "WISEPAD_3"],
    },
    {
      "ordinal": 1,
      "name": "INTERNET",
      "devices": ["WISEPOS_E", "VERIFONE_P400"]
    },
    {
      "ordinal": 2,
      "name": "LOCAL_MOBILE",
      "devices": []
    },
    {
      "ordinal": 3,
      "name": "HANDOFF",
      "devices": []
    },
    {
      "ordinal": 4,
      "name": "EMBEDDED",
      "devices": []
    },
    {
      "ordinal": 5,
      "name": "USB",
      "devices": []
    }
  ],
  "PaymentIntentStatus": {
    SUCCEEDED: {
      "ordinal": 0,
      "name": "SUCCEEDED",
      "display": "Succeeded"
    },
    REQUIRES_PAYMENT_METHOD: {
      "ordinal": 1,
    }
  },
  "SimulatedCardType" : [
    {
      "ordinal": 0,

    }
  ]
}
