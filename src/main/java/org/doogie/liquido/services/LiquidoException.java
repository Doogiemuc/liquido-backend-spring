package org.doogie.liquido.services;

import org.springframework.http.HttpStatus;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
//TOOD: different errors shall have different response status @ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class LiquidoException extends Exception {

  Errors error;

	public enum Errors {
		UNAUTHORIZED(1, HttpStatus.UNAUTHORIZED),          					// when client tries to call something without being authenticated!
	  CANNOT_REGISTER(2, HttpStatus.BAD_REQUEST),									// error in registration
		MOBILE_NOT_FOUND(3, HttpStatus.UNAUTHORIZED),								// mobile number when logging in via SMS not found
		CANNOT_LOGIN(4, HttpStatus.UNAUTHORIZED),										// when trying to logging in via SMS or E-Mail link
		INVALID_VOTER_TOKEN(5, HttpStatus.UNAUTHORIZED),
  	CANNOT_CREATE_POLL(6, HttpStatus.BAD_REQUEST),
		CANNOT_JOIN_POLL(7, HttpStatus.BAD_REQUEST),
    CANNOT_ADD_PROPOSAL(8, HttpStatus.BAD_REQUEST),
    CANNOT_START_VOTING_PHASE(9, HttpStatus.BAD_REQUEST),
		CANNOT_SAVE_PROXY(10, HttpStatus.BAD_REQUEST),								// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(11, HttpStatus.BAD_REQUEST),
		CANNOT_CAST_VOTE(12, HttpStatus.BAD_REQUEST),
    CANNOT_GET_TOKEN(13, HttpStatus.BAD_REQUEST),
		CANNOT_FINISH_POLL(14, HttpStatus.BAD_REQUEST),
		NO_DELEGATION(15, HttpStatus.BAD_REQUEST),
		CANNOT_FIND_ENTITY(16, HttpStatus.UNPROCESSABLE_ENTITY),   		// 422: cannot find entity: e.g. from PathParam or when Deserializing
		INVALID_POLL_STATUS(17, HttpStatus.BAD_REQUEST),
		INVALID_JWT_TOKEN(18, HttpStatus.UNAUTHORIZED),
		PUBLIC_CHECKSUM_NOT_FOUND(10, HttpStatus.NOT_FOUND);
		int liquidoErrorCode;
		HttpStatus httpResponseStatus;

    Errors(int code, HttpStatus httpResponseStatus) {
    	this.liquidoErrorCode = code;
    	this.httpResponseStatus = httpResponseStatus;
    }
  }

	/**
	 * A Liquido exception must always have an error code and a human readable error message
	 */
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
    return this.error.liquidoErrorCode;
  }

  public String getErrorName() {
    return this.error.name();
  }

	public HttpStatus getHttpResponseStatus() {
		return this.error.httpResponseStatus;
	}

  public String toString() {
  	return "LiquidoException[liquidoErrorCode="+this.getErrorCodeAsInt()+", errorName="+this.getErrorName()+", msg='"+this.getMessage()+"']";
	}
}
