package org.doogie.liquido.services;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
public class LiquidoException extends Exception {

  Errors errorCode;

  public enum Errors {
    CANNOT_CREATE_POLL(0),
    CANNOT_ADD_PROPOSAL(1),
    CANNOT_START_VOTING_PHASE(2);

    int errorCode;
    Errors(int code) { this.errorCode = code; }
  }

  public LiquidoException(Errors errCode) {
    super(errCode.toString());
    this.errorCode = errCode;
  }

  public LiquidoException(Errors errCode, String msg) {
    super(msg);
    this.errorCode = errCode;
  }

}
