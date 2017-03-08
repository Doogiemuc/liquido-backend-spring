package org.doogie.liquido.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception class that can be returned when business validation fails.
 * Will return HttpStatus.BAD_REQUEST  (instead of "internal server error" that a normal exception would set.)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LiquidoRestException extends RuntimeException {

  int error_code;

  public LiquidoRestException() {}

  //TODO: remove this constructor and only allow errors with code
  public LiquidoRestException(String message) {
    super(message);
  }

  public LiquidoRestException(String message, int code) {
    super(message);
    this.error_code = code;
  }

  public LiquidoRestException(String message, Throwable cause) {
    super(message, cause);
  }
}
