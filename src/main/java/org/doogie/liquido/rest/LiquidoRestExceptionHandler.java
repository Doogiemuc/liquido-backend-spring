package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

/**
 * Global Liquido REST exception handler
 * This exception handler returns a nice JSON for each error.
 * And it also returns the corresponding HttpStatus code depending on the errorCode
 *
 * @See {@link LiquidoException}  and {@link LiquidoGeneralExceptionHandler}
 * @See https://www.baeldung.com/exception-handling-for-rest-with-spring
 */
@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LiquidoRestExceptionHandler extends ResponseEntityExceptionHandler {

	// Another possible implementation could be   https://www.mkyong.com/spring-boot/spring-rest-error-handling-example/
	//   @Component
	//   public class CustomErrorAttributes extends DefaultErrorAttributes { ... }

	@Autowired
	Environment springEnv;

	/* BUGFIX: If I do this then LiquidoException is not handled anymore.
	@ExceptionHandler(value = {Exception.class})
	ResponseEntity<Object> handleGeneralException(Exception e, WebRequest request) {
		log.warn("Internal server error", e);
		return handleExceptionInternal(e, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
	}
	*/

	/**
	 * This will be called when a {@link LiquidoException} is thrown.
	 * @param lex the LiquidoException with a specific Error code
	 * @param request the HTTP request
	 * @return an Http ResponseEntity with error info in body, that will be returned to the client.
	 */
	@ExceptionHandler(value = {LiquidoException.class})
	ResponseEntity<Object> handleLiquidoException(LiquidoException lex, WebRequest request) {
		//log.info(lex.toString());
		Lson bodyOfResponse = getMessageAsJson(lex, request);
		if (lex.getHttpResponseStatus().is4xxClientError() ||
			lex.getHttpResponseStatus().is4xxClientError()) {
			log.warn(lex.toString());  // If something more bad happens, then log it. This is a sophisticated toString() that logs usefull info.
		} else
		if (lex.getHttpResponseStatus().is5xxServerError() ||
		    lex.getHttpResponseStatus().is5xxServerError()) {
			log.error(lex.toString());  // If something more bad happens, then log it. This is a sophisticated toString() that logs usefull info.
		}
		return handleExceptionInternal(lex, bodyOfResponse.toString(), new HttpHeaders(), lex.getHttpResponseStatus(), request);
	}

	/**
	 * This will be called for any generel HTTP exception. (LiquidoException or others)
	 * @param ex
	 * @param body
	 * @param headers
	 * @param status
	 * @param request
	 * @return
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

		if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
			log.error("Internal server error "+ex.toString());
			request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
		} else {
			log.debug("Liquido Rest throws: "+ex.toString());
		}
		if (body == null) {
			body = getMessageAsJson(ex, request);
		}
		if (log.isDebugEnabled()) {
			try {
				log.debug(body.toString());
				//StackTraceElement[] firstElems = Arrays.copyOfRange(ex.getStackTrace(), 0, Math.min(5, ex.getStackTrace().length));
				//bodyOfResponse.put("stacktrace", firstElems);    // this will be nicely serialized as JSON by my Lson utility   => but I decided to NOT return exceptions to the client!
				// Log first 5 elements of stacktrace
				for (int i = 0; i < 5; i++) {
					if (ex.getStackTrace().length > i) {
						log.debug("    at "+ex.getStackTrace()[i].toString());
					}
				}
			} catch (Throwable ignore) {
				log.error("something very bad happened.");
			}
		}
		return new ResponseEntity<>(body, headers, status);
	}

	/**
	 * Create a body for an error response. This tries to be the one main central method where we build error
	 * responses for the whole liquido backend API. It's actually quite hard to add it to all thinkable ways
	 * that spring boot may respond to requests. ControllerAdvice is simple. But it is way harder e.g. for
	 * exceptions ins filters
	 *
	 * Here we try to return as much usefull information as possible.
	 * @param ex any exception or a LiquidoException
	 * @param request normally a ServletWebRequest in our REST world.
	 * @return JSON for body
	 */
	public static Lson getMessageAsJson(Exception ex, WebRequest request) {
		Lson bodyOfResponse;

		if (ex instanceof LiquidoException) {
			bodyOfResponse = ((LiquidoException)ex).toLson();
		} else {
			bodyOfResponse = Lson.builder()
				.put("exception", ex.getClass().toString())
				.put("message", ex.getMessage());
		}
		if (ex.getCause() != null && ex.getCause().getMessage() != null) {
			bodyOfResponse.put("cause", ex.getCause().getMessage());
		}

		// Let's try to add some more valuable info if request is a ServletWebRequest
		try {
			bodyOfResponse.put("httpMethod", ((ServletWebRequest) request).getHttpMethod());
			String requestURL  = ((ServletWebRequest)request).getRequest().getRequestURL().toString();
			String queryString = ((ServletWebRequest)request).getRequest().getQueryString();
			if (!DoogiesUtil.isEmpty(queryString)) requestURL += "?" + queryString;
			if (!DoogiesUtil.isEmpty(requestURL)) bodyOfResponse.put("requestURL", requestURL);
			bodyOfResponse.put("remoteUser", request.getRemoteUser());
		} catch (Throwable ignore) { }

		return bodyOfResponse;
	}



}