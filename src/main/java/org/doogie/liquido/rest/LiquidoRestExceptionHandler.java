package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.DoogiesUtil;
import org.doogie.liquido.util.Lson;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
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

	@ExceptionHandler(value = {LiquidoException.class})
	protected ResponseEntity<Object> handleConflict(LiquidoException lex, WebRequest request) {
		log.info("REST error response: "+lex);
		Lson bodyOfResponse = Lson.builder()
				.put("liquidoErrorCode", lex.getErrorCodeAsInt())
				.put("liquidoErrorName", lex.getErrorName())
				.put("message", lex.getMessage())
				 .put("httpStatus", lex.getHttpResponseStatus().value());

		// Let's try to add some more valuable info
		try {
			// request URL with query parameters
			String requestURL = ((ServletWebRequest)request).getRequest().getRequestURL().toString();
			String queryString = ((ServletWebRequest)request).getRequest().getQueryString();
			if (!DoogiesUtil.isEmpty(queryString)) requestURL += "?" + queryString;
			if (!DoogiesUtil.isEmpty(requestURL)) bodyOfResponse.put("requestURL", requestURL);

			// first fiveElems of stack trace
			StackTraceElement[] firstElems = Arrays.copyOfRange(lex.getStackTrace(), 0, Math.min(5, lex.getStackTrace().length));
			bodyOfResponse.put("stacktrace", firstElems);		// this will be nicely serialized as JSON by my Lson utility
  	} catch (Throwable ignore) {}

		return handleExceptionInternal(lex, bodyOfResponse.toString(), new HttpHeaders(), lex.getHttpResponseStatus(), request);
	}

}
