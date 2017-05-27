package org.doogie.liquido.services;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
public class LiquidoException extends Exception {

  public LiquidoException(String msg) {
    super(msg);
  }

}
