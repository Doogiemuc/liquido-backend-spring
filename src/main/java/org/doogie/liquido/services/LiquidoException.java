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
	  CANNOT_REGISTER(2),
	  MUST_REGISTER(3),           // login attempt with nonexistent phonenumber or email
		INVALID_VOTER_TOKEN(4),
  	CANNOT_CREATE_POLL(5),
		CANNOT_JOIN_POLL(6),
    CANNOT_ADD_PROPOSAL(7),
    CANNOT_START_VOTING_PHASE(8),
		CANNOT_SAVE_PROXY(9),				// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(10),
		CANNOT_CAST_VOTE(11),
    CANNOT_GET_TOKEN(12),
		CANNOT_FINISH_POLL(13),
		NO_DELEGATION(14),
		CANNOT_FIND_ENTITY(15),   // cannot find entity when deserializing
	  CANNOT_LOGIN(16),
		INVALID_POLL_STATUS(17),
		INVALID_JWT_TOKEN(18);
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
