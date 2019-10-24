package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.util.Lson;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

/**
 * Handle general internal server errors and any Exception that was not caught otherwise.
 * @See {@link LiquidoRestExceptionHandler}
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class LiquidoGeneralExceptionHandler {

	//TODO: Now also Resource not found is a 500 instead of 404 :-(  TODO: only call my really generalException Method  AFTER RepositoryRestExceptionHandler has done its job.

	//TODO: This is normally handeld by RepositoryRestExceptionHandler.  But I want to catch EVERY exception below => MAYBE check Order
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseBody
	ResponseEntity handleDataIntegrityViolationException(Exception re, WebRequest request) {
		Lson body = LiquidoRestExceptionHandler.getMessageAsJson(re, request);
		body.put("message", "This operation is not allowed due to data integrity");
		body.put("messageInternal", re.getMessage());
		log.debug("LIQUIDO Data Integrity Violation Exception\n"+body.toPrettyString());
		return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.CONFLICT);
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	ResponseEntity handleGeneralException(Exception re, WebRequest request) {
		Lson body = LiquidoRestExceptionHandler.getMessageAsJson(re, request);
		body.put("message", "There was an internal error in Liquido. We are sorry for that.");
		body.put("messageInternal", re.getMessage());
		log.warn("LIQUIDO Internal Server Error\n"+body.toPrettyString());
		return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
	}

}
