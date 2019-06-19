package org.doogie.liquido.services;

import org.doogie.liquido.util.Lson;
import org.springframework.http.HttpStatus;

/**
 * General purpose exception for functional ("business") Exceptions.
 * For example for inconsistent state.
 */
public class LiquidoException extends Exception {

  Errors error;

	public enum Errors {
		UNAUTHORIZED(1, HttpStatus.UNAUTHORIZED),          					// when client tries to call something without being authenticated!
	  CANNOT_REGISTER(2, HttpStatus.BAD_REQUEST),									// error in registration
		USER_EXISTS(3, HttpStatus.CONFLICT),												// user with that email or mobile phonen umber already exists
		MOBILE_NOT_FOUND(4, HttpStatus.UNAUTHORIZED),								// mobile number when logging in via SMS not found
		CANNOT_LOGIN(5, HttpStatus.UNAUTHORIZED),										// when trying to logging in via SMS or E-Mail link
		INVALID_VOTER_TOKEN(6, HttpStatus.UNAUTHORIZED),
  	CANNOT_CREATE_POLL(7, HttpStatus.BAD_REQUEST),
		CANNOT_JOIN_POLL(8, HttpStatus.BAD_REQUEST),
    CANNOT_ADD_PROPOSAL(9, HttpStatus.BAD_REQUEST),
    CANNOT_START_VOTING_PHASE(10, HttpStatus.BAD_REQUEST),
		CANNOT_SAVE_PROXY(11, HttpStatus.BAD_REQUEST),								// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(12, HttpStatus.BAD_REQUEST),
		CANNOT_CAST_VOTE(13, HttpStatus.BAD_REQUEST),
    CANNOT_GET_TOKEN(14, HttpStatus.BAD_REQUEST),
		CANNOT_FINISH_POLL(15, HttpStatus.BAD_REQUEST),
		NO_DELEGATION(16, HttpStatus.BAD_REQUEST),
		CANNOT_FIND_ENTITY(17, HttpStatus.UNPROCESSABLE_ENTITY),   		// 422: cannot find entity: e.g. from PathParam or when Deserializing
		NO_BALLOT(18, HttpStatus.NO_CONTENT),  												// 204: voter has no ballot yet. This is OK and not an error.
		INVALID_POLL_STATUS(19, HttpStatus.BAD_REQUEST),
		JWT_TOKEN_INVALID(20, HttpStatus.UNAUTHORIZED),
		JWT_TOKEN_EXPIRED(21, HttpStatus.UNAUTHORIZED),
		PUBLIC_CHECKSUM_NOT_FOUND(22, HttpStatus.NOT_FOUND),
		CANNOT_ADD_SUPPORTER(23, HttpStatus.BAD_REQUEST);						// e.g. when user tries to support his own proposal

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

	public Lson toLson() {
		return Lson.builder()
			.put("exception", this.getClass().toString())
			.put("message", this.getMessage())
			.put("liquidoErrorCode", this.getErrorCodeAsInt())
			.put("liquidoErrorName", this.getErrorName())
			.put("httpStatus", this.getHttpResponseStatus().value())
		  .put("httpStatusName", this.getHttpResponseStatus().getReasonPhrase());
	}

  public String toString() {
  	return "LiquidoException[liquidoErrorCode="+this.getErrorCodeAsInt()+", errorName="+this.getErrorName()+", msg='"+this.getMessage()+"']";
	}
}
