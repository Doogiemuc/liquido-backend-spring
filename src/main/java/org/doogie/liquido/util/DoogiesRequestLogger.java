package org.doogie.liquido.util;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

/**
 * Doogies very cool HTTP request logging.
 * Yes I am completely fanatic about logging HTTP requests. But believe me, this has saved me hours of debugging.
 *
 * There is also {@link org.springframework.web.filter.CommonsRequestLoggingFilter}  but it cannot log request method
 * And it cannot easily be extended.
 *
 * https://mdeinum.wordpress.com/2015/07/01/spring-framework-hidden-gems/
 * http://stackoverflow.com/questions/8933054/how-to-read-and-copy-the-http-servlet-response-output-stream-content-for-logging
 */
public class DoogiesRequestLogger extends OncePerRequestFilter {

  private boolean includeResponsePayload = true;
  private boolean logRequestHeaders = true;
  private int maxPayloadLength = 1000;

  private String getContentAsString(byte[] buf, int maxLength, String charsetName) {
    if (buf == null || buf.length == 0) return "";
    int length = Math.min(buf.length, this.maxPayloadLength);
    try {
      String s = new String(buf, 0, length, charsetName);
      if (buf.length > maxLength) s = s + "...";
      return s;
    } catch (UnsupportedEncodingException ex) {
      return "Unsupported Encoding";
    }
  }

  /**
   * Log each request and respponse with full Request URI, content payload and duration of the request in ms.
   * @param request the request
   * @param response the response
   * @param filterChain chain of filters
   * @throws ServletException sometimes
   * @throws IOException some other times
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

    long startTime = System.currentTimeMillis();

    String requestId = '[' + String.valueOf(startTime % 10000) + ']';

    StringBuilder reqInfo = new StringBuilder()
     .append(requestId)
     .append(" ")
     .append(request.getMethod())
     .append(" ")
     .append(request.getRequestURL());

    StringBuilder reqInfoLong = new StringBuilder(reqInfo.toString());

    String queryString = request.getQueryString();
    if (queryString != null) {
      reqInfoLong.append("?").append(queryString);
    }

    if (request.getAuthType() != null) {
      reqInfoLong.append(", authType=")
        .append(request.getAuthType());
    }
    if (request.getUserPrincipal() != null) {
      reqInfoLong.append(", principalName=")
        .append(request.getUserPrincipal().getName());
    }
    if (request.getRemoteUser() != null) {
      reqInfoLong.append(", remoteUser=").append(request.getRemoteUser());
    }
    if (request.getSession() !=  null) {
      reqInfoLong.append(", sessionId=").append(request.getSession().getId());
    }

    this.logger.debug("=> " + reqInfoLong);

    if (logRequestHeaders) {
    	this.logger.debug("   Request Headers:");
	    Enumeration<String> hnames = request.getHeaderNames();
      while (hnames.hasMoreElements()) {
      	String name  = hnames.nextElement();
				String value = request.getHeader(name);
				this.logger.debug("     "+name+"="+value);
      }
    }

    // ========= Log request and response payload ("body") ========
    // We CANNOT simply read the request payload here, because then the InputStream would be consumed and cannot be read again by the actual processing/server.
    // DO NOT DO THIS:  this.logger.debug("Request body: "+DoogiesUtil._stream2String(request.getInputStream()));




    // So we need to apply some stronger magic here :-)
    ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
    ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

    //TODO: can I first log the request body and then reset the reader like this?   wrappedRequest.getReader().reset();

    filterChain.doFilter(wrappedRequest, wrappedResponse);     // ======== This performs the actual request!
    long duration = System.currentTimeMillis() - startTime;

    // I can only log the request's body AFTER the request has been made and ContentCachingRequestWrapper did its work.
    String requestBody = this.getContentAsString(wrappedRequest.getContentAsByteArray(), this.maxPayloadLength, request.getCharacterEncoding());
    if (requestBody.length() > 0) {
      this.logger.debug("   "+requestId+" Request body was: " +requestBody);
      if (requestBody.length() > maxPayloadLength)
        this.logger.debug("[...]");
    } else {
    	if ("POST".equals(request.getMethod())) {
    		this.logger.debug("   "+reqInfo+" EMPTY request body posted");
	    }
    }

    this.logger.debug("<= " + reqInfo + " returned " + response.getStatus() + " in "+duration + "ms.");
    if (includeResponsePayload) {
      byte[] buf = wrappedResponse.getContentAsByteArray();
      this.logger.debug("   returned response body:\n"+getContentAsString(buf, this.maxPayloadLength, response.getCharacterEncoding()));
    }

    wrappedResponse.copyBodyToResponse();  // IMPORTANT: copy content of response back into original response
  }

	/* Spring own implementation is nearly ok, but it cannot log the request type
	@Bean
	public CommonsRequestLoggingFilter requestLoggingFilter() {
		log.debug("Configuring request logging filter");
		CommonsRequestLoggingFilter crlf = new CommonsRequestLoggingFilter();
		crlf.setBeforeMessagePrefix(" => [");
		crlf.setAfterMessagePrefix(" <= [");
		crlf.setIncludeClientInfo(true);
		crlf.setIncludeHeaders(true);
		crlf.setIncludeQueryString(true);
		crlf.setIncludePayload(true);
		crlf.setMaxPayloadLength(1000);
		return crlf;
	}
*/

}
