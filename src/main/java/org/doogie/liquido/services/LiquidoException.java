package org.doogie.liquido.services;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
//TOOD: different errors shall have different response status @ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class LiquidoException extends Exception {

  Errors error;

	public enum Errors {
		NO_LOGIN(1, HttpStatus.UNAUTHORIZED),          							// when someone tries to call something without being authenticated!
	  CANNOT_REGISTER(2, HttpStatus.BAD_REQUEST),
		CANNOT_LOGIN(16, HttpStatus.UNAUTHORIZED),									// when trying to logging in via SMS or E-Mail link
		INVALID_VOTER_TOKEN(4, HttpStatus.UNAUTHORIZED),
  	CANNOT_CREATE_POLL(5, HttpStatus.BAD_REQUEST),
		CANNOT_JOIN_POLL(6, HttpStatus.BAD_REQUEST),
    CANNOT_ADD_PROPOSAL(7, HttpStatus.BAD_REQUEST),
    CANNOT_START_VOTING_PHASE(8, HttpStatus.BAD_REQUEST),
		CANNOT_SAVE_PROXY(9, HttpStatus.BAD_REQUEST),								// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(10, HttpStatus.BAD_REQUEST),
		CANNOT_CAST_VOTE(11, HttpStatus.BAD_REQUEST),
    CANNOT_GET_TOKEN(12, HttpStatus.BAD_REQUEST),
		CANNOT_FINISH_POLL(13, HttpStatus.BAD_REQUEST),
		NO_DELEGATION(14, HttpStatus.BAD_REQUEST),
		CANNOT_FIND_ENTITY(15, HttpStatus.BAD_REQUEST),   					// cannot find entity when deserializing
		INVALID_POLL_STATUS(17, HttpStatus.BAD_REQUEST),
		INVALID_JWT_TOKEN(18, HttpStatus.UNAUTHORIZED);
		int errorCode;
		HttpStatus responseStatus;

    Errors(int code, HttpStatus responseStatus) {
    	this.errorCode = code;
    	this.responseStatus = responseStatus;
    }
  }

  public LiquidoException(Errors errCode, String msg) {
    super(msg);
    this.error = errCode;
  }

  public LiquidoException(Errors errCode, String msg, Throwable childException) {
  	super(msg, childException);
	  this.error = errCode;
  }

  public Errors getError() {
  	return this.error;
	}

  public int getErrorCodeAsInt() {
    return this.error.errorCode;
  }

  public String getErrorName() {
    return this.error.name();
  }

	public HttpStatus getHttpResponseStatus() {
		return this.error.responseStatus;
	}

  public String toString() {
  	return "LiquidoException[errorCode="+this.getErrorCodeAsInt()+", errorName="+this.getErrorName()+", msg='"+this.getMessage()+"']";
	}
}
