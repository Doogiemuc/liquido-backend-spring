package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import java.util.Arrays;

/**
 * Global Liquido REST exception handler
 * This exception handler returns a nice JSON for each error.
 * And it also returns the corresponding HttpStatus code depending on the errorCode
 * @See LiquidoException
 */
@Slf4j
@ControllerAdvice
public class LiquidoRestExceptionHandler extends ResponseEntityExceptionHandler {

	@Autowired
	Environment springEnv;

	@ExceptionHandler(value = {LiquidoException.class})
	ResponseEntity<Object> handleLiquidoException(LiquidoException lex, WebRequest request) {
		//log.info(lex.toString());
		Lson bodyOfResponse = getMessageAsJson(lex, request);
		return handleExceptionInternal(lex, bodyOfResponse.toString(), new HttpHeaders(), lex.getHttpResponseStatus(), request);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
		log.debug("REST Exception "+ex.toString());
		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		}
		if (body == null) {
			body = getMessageAsJson(ex, request);
		}
		return new ResponseEntity<>(body, headers, status);
	}

	/**
	 * Create a body for an error response.
	 * Here we try to return as much usefull information as possible.
	 * @param ex any exception or a LiquidoException
	 * @param request normally a ServletWebRequest in our REST world.
	 * @return JSON for body
	 */
	private Lson getMessageAsJson(Exception ex, WebRequest request) {
		Lson bodyOfResponse = Lson.builder()
				.put("exception", ex.getClass().toString())
				.put("message", ex.getMessage());

		if (ex.getCause() != null && ex.getCause().getMessage() != null) {
			bodyOfResponse.put("cause", ex.getCause().getMessage());
		}

		if (ex instanceof LiquidoException) {
			LiquidoException lex = (LiquidoException)ex;
			bodyOfResponse
					.put("liquidoErrorCode", lex.getErrorCodeAsInt())
					.put("liquidoErrorName", lex.getErrorName())
					.put("httpStatus", lex.getHttpResponseStatus().value());
		}

		// Let's try to add some more valuable info if request is a ServletWebRequest
		try {
			String requestURL  = ((ServletWebRequest)request).getRequest().getRequestURL().toString();
			String queryString = ((ServletWebRequest)request).getRequest().getQueryString();
			if (!DoogiesUtil.isEmpty(queryString)) requestURL += "?" + queryString;
			if (!DoogiesUtil.isEmpty(requestURL)) bodyOfResponse.put("requestURL", requestURL);
			bodyOfResponse.put("remoteUser", request.getRemoteUser());
		} catch (Throwable ignore) { }

		// add first fiveElems of stack trace   (when DEV or TEST env)
		if (springEnv.acceptsProfiles(Profiles.of("DEV", "TEST"))) {
			try {
				StackTraceElement[] firstElems = Arrays.copyOfRange(ex.getStackTrace(), 0, Math.min(5, ex.getStackTrace().length));
				bodyOfResponse.put("stacktrace", firstElems);    // this will be nicely serialized as JSON by my Lson utility
			} catch (Throwable ignore) {
			}
		}

		return bodyOfResponse;
	}



}