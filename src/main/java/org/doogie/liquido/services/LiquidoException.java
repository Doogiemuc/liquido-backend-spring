package org.doogie.liquido.services;

import org.doogie.liquido.util.Lson;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Liquido general purpose exception.
 * There is quite some logic in here.
 * Each LiquidoException MUST contain an error code. This error code also decides which HTTP status code will be returned
 * to the client.
 */
public class LiquidoException extends Exception {

	/** LIQUIDO error code */
  Errors error;

  /**
	 * Additional key/value payload that can be added to the exception.
	 * Do not add sensitive data to this!!!
	 * This data will be serialized to the resulting JSON at will be returned to the client.
	 */
  Map payload;

	/**
	 * These codes are pretty fine grained. The idea here is that the client can show
	 * usefull and localized messages to a human depending on these codes.
	 */
	public enum Errors {
		CANNOT_REGISTER_NEED_EMAIL(1, HttpStatus.BAD_REQUEST),
		CANNOT_REGISTER_NEED_MOBILEPHONE(2, HttpStatus.BAD_REQUEST),

		// Create New Team
		TEAM_WITH_SAME_NAME_EXISTS(10, HttpStatus.CONFLICT),
		CANNOT_CREATE_TEAM_ALREADY_REGISTERED(11, HttpStatus.CONFLICT),			// Edge case: When a user is already registered and want's to create a team, ...
		// Join a team
		CANNOT_JOIN_TEAM_INVITE_CODE_INVALID(12, HttpStatus.BAD_REQUEST),
		CANNOT_JOIN_TEAM_ALREADY_MEMBER(13, HttpStatus.CONFLICT),						// there already is a member (or admin) with the same email or mobilephone
		CANNOT_JOIN_TEAM_ALREADY_ADMIN(14, HttpStatus.CONFLICT),
		CANNOT_CREATE_TWILIO_USER(15, HttpStatus.INTERNAL_SERVER_ERROR),
		USER_EMAIL_EXISTS(16, HttpStatus.CONFLICT),                         // user with that email already exists
		USER_MOBILEPHONE_EXISTS(17, HttpStatus.CONFLICT),                   // user with that mobilephone already exists

		//Login Errors
		CANNOT_LOGIN_MOBILE_NOT_FOUND(20, HttpStatus.UNAUTHORIZED),					// when requesting an SMS login token and mobile number is not known
		CANNOT_LOGIN_EMAIL_NOT_FOUND(21, HttpStatus.UNAUTHORIZED),   				// when requesting a login email and email is not known
		CANNOT_LOGIN_TOKEN_INVALID(22, HttpStatus.UNAUTHORIZED),     				// when a email or sms login token is invalid or expired
		CANNOT_LOGIN_TEAM_NOT_FOUND(23, HttpStatus.UNAUTHORIZED),           // when changing team
		CANNOT_LOGIN_USER_NOT_MEMBER_OF_TEAM(24, HttpStatus.UNAUTHORIZED),  // when changing team and user is not member or admin of target team
		CANNOT_LOGIN_INTERNAL_ERROR(25, HttpStatus.INTERNAL_SERVER_ERROR),	// when sending of email is not possible
		CANNOT_REQUEST_SMS_TOKEN(26, HttpStatus.UNAUTHORIZED),              // eg. when entered mobile number is not valid

		//JWT Errors
		JWT_TOKEN_INVALID(30, HttpStatus.UNAUTHORIZED),
		JWT_TOKEN_EXPIRED(31, HttpStatus.UNAUTHORIZED),

		// use case errors
		INVALID_VOTER_TOKEN(50, HttpStatus.UNAUTHORIZED),
		CANNOT_CREATE_POLL(51, HttpStatus.BAD_REQUEST),
		CANNOT_JOIN_POLL(52, HttpStatus.BAD_REQUEST),
		CANNOT_ADD_PROPOSAL(53, HttpStatus.BAD_REQUEST),
		CANNOT_START_VOTING_PHASE(54, HttpStatus.BAD_REQUEST),
		CANNOT_SAVE_PROXY(55, HttpStatus.BAD_REQUEST),								// assign or remove
		CANNOT_ASSIGN_CIRCULAR_PROXY(56, HttpStatus.BAD_REQUEST),
		CANNOT_CAST_VOTE(57, HttpStatus.BAD_REQUEST),
		CANNOT_GET_TOKEN(58, HttpStatus.BAD_REQUEST),
		CANNOT_FINISH_POLL(59, HttpStatus.BAD_REQUEST),
		NO_DELEGATION(60, HttpStatus.BAD_REQUEST),
		NO_BALLOT(61, HttpStatus.NO_CONTENT),  												// 204: voter has no ballot yet. This is OK and not an error.
		INVALID_POLL_STATUS(62, HttpStatus.BAD_REQUEST),
		PUBLIC_CHECKSUM_NOT_FOUND(63, HttpStatus.NOT_FOUND),
		CANNOT_ADD_SUPPORTER(64, HttpStatus.BAD_REQUEST),							// e.g. when user tries to support his own proposal

		CANNOT_CALCULATE_UNIQUE_RANKED_PAIR_WINNER(70, HttpStatus.INTERNAL_SERVER_ERROR),		// this is only used in the exceptional situation, that no unique winner can be calculated in RankedPairVoting
		CANNOT_VERIFY_CHECKSUM(80, HttpStatus.NOT_FOUND),							// ballot's checksum could not be verified

		// general errors
		GRAPHQL_ERROR(400, HttpStatus.BAD_REQUEST),											// e.g. missing required fields, invalid GraphQL query, ...
		UNAUTHORIZED(401, HttpStatus.UNAUTHORIZED),          					  // when client tries to call something without being authenticated!
		CANNOT_FIND_ENTITY(404, HttpStatus.NOT_FOUND),   								// 404: cannot find entity
		INTERNAL_ERROR(500, HttpStatus.INTERNAL_SERVER_ERROR);

		int liquidoErrorCode;
		HttpStatus httpResponseStatus;

		Errors(int code, HttpStatus httpResponseStatus) {
			this.liquidoErrorCode = code;
			this.httpResponseStatus = httpResponseStatus;
		}

		int getLiquidoErrorCode() {
			return this.liquidoErrorCode;
		}
		HttpStatus getHttpResponseStatus() {
			return this.httpResponseStatus;
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

	public LiquidoException(Errors errCode, String msg, Throwable childException, Map payload) {
		this(errCode, msg, childException);
		this.payload = payload;
	}


	/**
	 * This can be used like this
	 * <pre>Optional.orElseThrow(LiquidoException.notFound("not found"))</pre>
	 * @param msg
	 * @return
	 */
  public static Supplier<LiquidoException> notFound(String msg) {
		return () -> new LiquidoException(Errors.CANNOT_FIND_ENTITY, msg);
	}

	public static Supplier<LiquidoException> unauthorized(String msg)  {
		return () -> new LiquidoException(Errors.UNAUTHORIZED, msg);
	}

	public static Supplier<LiquidoException> supply(Errors error, String msg) {
	return () -> new LiquidoException(error, msg);
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
		Lson lson = Lson.builder()
			.put("exception", this.getClass().toString())
			.put("message", this.getMessage())
			.put("liquidoErrorCode", this.getErrorCodeAsInt())
			.put("liquidoErrorName", this.getErrorName())
			.put("httpStatus", this.getHttpResponseStatus().value())
		  .put("httpStatusName", this.getHttpResponseStatus().getReasonPhrase());

		if (this.getCause() != null) lson.put("cause", this.getCause().toString());
		if (this.payload != null && payload.size() > 0) lson.put("liquidoErrorPayload", this.payload);

		return lson;
	}

  public String toString() {
	StringBuilder b = new StringBuilder("LiquidoException[");
	b.append("liquidoErrorCode=");
	b.append(this.getErrorCodeAsInt());
	b.append(", errorName=");
	b.append(this.getErrorName());
	b.append(", msg=");
	b.append(this.getMessage());
	if (this.getCause() != null) {
		b.append(", cause=");
		b.append(this.getCause().toString());
		}
	b.append("]");
	return b.toString();
	}
}
