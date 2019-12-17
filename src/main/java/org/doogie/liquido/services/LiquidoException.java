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
		CANNOT_LOGIN_MOBILE_NOT_FOUND(4, HttpStatus.UNAUTHORIZED),	// when requesting an SMS login token and mobile number is not known
		CANNOT_LOGIN_EMAIL_NOT_FOUND(5, HttpStatus.UNAUTHORIZED),   // when requesting a login email and email is not known
		CANNOT_LOGIN_TOKEN_INVALID(6, HttpStatus.UNAUTHORIZED),     // when a email or sms login token is invalid or expired
		CANNOT_LOGIN_INTERNAL_ERROR(7, HttpStatus.INTERNAL_SERVER_ERROR),  // when sending of email is not possible
		INVALID_VOTER_TOKEN(8, HttpStatus.UNAUTHORIZED),
  	CANNOT_CREATE_POLL(9, HttpStatus.BAD_REQUEST),
		CANNOT_JOIN_POLL(10, HttpStatus.BAD_REQUEST),
    CANNOT_ADD_PROPOSAL(11, HttpStatus.BAD_REQUEST),
    CANNOT_START_VOTING_PHASE(12, HttpStatus.BAD_REQUEST),
		CANNOT_SAVE_PROXY(13, HttpStatus.BAD_REQUEST),								// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(14, HttpStatus.BAD_REQUEST),
		CANNOT_CAST_VOTE(15, HttpStatus.BAD_REQUEST),
    CANNOT_GET_TOKEN(16, HttpStatus.BAD_REQUEST),
		CANNOT_FINISH_POLL(17, HttpStatus.BAD_REQUEST),
		NO_DELEGATION(18, HttpStatus.BAD_REQUEST),
		CANNOT_FIND_ENTITY(19, HttpStatus.UNPROCESSABLE_ENTITY),   		// 422: cannot find entity: e.g. from PathParam or when Deserializing
		NO_BALLOT(20, HttpStatus.NO_CONTENT),  												// 204: voter has no ballot yet. This is OK and not an error.
		INVALID_POLL_STATUS(21, HttpStatus.BAD_REQUEST),
		JWT_TOKEN_INVALID(22, HttpStatus.UNAUTHORIZED),
		JWT_TOKEN_EXPIRED(23, HttpStatus.UNAUTHORIZED),
		PUBLIC_CHECKSUM_NOT_FOUND(24, HttpStatus.NOT_FOUND),
		CANNOT_ADD_SUPPORTER(25, HttpStatus.BAD_REQUEST),							// e.g. when user tries to support his own proposal
		CANNOT_CALCULATE_UNIQUE_RANKED_PAIR_WINNER(26, HttpStatus.INTERNAL_SERVER_ERROR),		// this is only used in the exceptional situation, that no unique winner can be calculated in RankedPairVoting
		CANNOT_VERIFY_CHECKSUM(27, HttpStatus.NOT_FOUND),							// ballot's checksum could not be verified
		INTERNAL_ERROR(99, HttpStatus.INTERNAL_SERVER_ERROR);

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
