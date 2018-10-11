package org.doogie.liquido.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class LiquidoException extends Exception {

  Errors error;

  public enum Errors {
		NO_LOGIN(1),                // when someone tries to call something without being authenticated!
		INVALID_VOTER_TOKEN(2),
  	CANNOT_CREATE_POLL(3),
    CANNOT_ADD_PROPOSAL(4),
    CANNOT_START_VOTING_PHASE(5),
		CANNOT_SAVE_PROXY(6),				// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(7),
		CANNOT_CAST_VOTE(8),
    CANNOT_GET_TOKEN(9);
		int errorCode;
    Errors(int code) { this.errorCode = code; }
  }

  public LiquidoException(Errors errCode) {
    super(errCode.toString());
    this.error = errCode;
  }

  public LiquidoException(Errors errCode, String msg) {
    super(msg);
    this.error = errCode;
  }

  public Errors getError() {
  	return this.error;
	}

  public int getErrorCodeAsInt() {
    return error.errorCode;
  }

  public String getErrorName() {
    return error.name();
  }
}
