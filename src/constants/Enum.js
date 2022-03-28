/**
 * Javascript Enum class, mimics the behavior of Java's Enum class.
 */

export default class Enum {
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




