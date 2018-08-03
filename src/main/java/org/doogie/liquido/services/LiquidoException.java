package org.doogie.liquido.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class LiquidoException extends Exception {

  Errors errorCode;

  public enum Errors {
    CANNOT_CREATE_POLL(0),
    CANNOT_ADD_PROPOSAL(1),
    CANNOT_START_VOTING_PHASE(2),
    USER_DOES_NOT_EXIST(3),
    NO_LOGIN(4),                // when someone tries to call something without being authenticated!
		CANNOT_SAVE_PROXY(5),
		CANNOT_CAST_VOTE(6),
    CANNOT_GET_TOKEN(7);
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

  public int getErrorCodeAsInt() {
    return errorCode.errorCode;
  }

  public String getErrorCodeName() {
    return errorCode.name();
  }
}
