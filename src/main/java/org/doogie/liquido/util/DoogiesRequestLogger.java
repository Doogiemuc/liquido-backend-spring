package org.doogie.liquido.util;

import com.amazonaws.services.dynamodbv2.xspec.B;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
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
 * Use your browsers developer console -> Network and compare to what the server receives.
 *
 * There is also {@link org.springframework.web.filter.CommonsRequestLoggingFilter}  but it cannot log request method
 * And it cannot easily be extended.
 *
 * https://mdeinum.wordpress.com/2015/07/01/spring-framework-hidden-gems/
 * http://stackoverflow.com/questions/8933054/how-to-read-and-copy-the-http-servlet-response-output-stream-content-for-logging
 */
@Component
public class DoogiesRequestLogger extends OncePerRequestFilter {

	@Value("${liquido.debug.log.includeResponsePayload:false}")
  private boolean includeResponsePayload;

	@Value("${liquido.debug.log.maxPayloadLength:1000}")
	private int maxPayloadLength;

	@Value("${liquido.debug.log.logRequestHeaders:false}")
  private boolean logRequestHeaders;

  private String getContentAsString(byte[] buf, int maxLength, String charsetName) {
    if (buf == null || buf.length == 0) return "";
    int length = Math.min(buf.length, this.maxPayloadLength);
    try {
      String s = new String(buf, 0, length, charsetName);
      if (buf.length > maxLength) s = s + " [...]";
      return s;
    } catch (UnsupportedEncodingException ex) {
      return "Unsupported Encoding";
    }
  }

  /**
   * Log each request and response with full Request URI, content payload and duration of the request in ms
	 * But only if log.isDebugEnabled()
	 *
   * @param request the request
   * @param response the response
   * @param filterChain chain of filters
   * @throws ServletException sometimes
   * @throws IOException some other times
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

  	if (!logger.isDebugEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}

    long startTime = System.currentTimeMillis();

    String requestId = '[' + String.valueOf(startTime % 10000) + ']';

    StringBuilder reqInfo = new StringBuilder()
     .append(requestId)
     .append(" ")
     .append(request.getMethod())
     .append(" ")
     .append(request.getRequestURL());

    String queryString = request.getQueryString();
    if (queryString != null) {
      reqInfo.append("?").append(queryString);
    }

	  StringBuilder reqInfoLong = new StringBuilder(reqInfo.toString());

    if (request.getHeader("Content-Type") != null) {
			reqInfoLong.append(", header.Content-Type=")
				.append(request.getHeader("Content-Type") );
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
				this.logger.debug("   - "+name+"="+value);
      }
    }

    // ========= Log request and response payload ("body") ========
    // We CANNOT simply read the request payload here, because then the InputStream would be consumed and cannot be read again by the actual processing/server.
    // DO NOT DO THIS:  this.logger.debug("Request body: "+DoogiesUtil._stream2String(request.getInputStream()));
    // Springs ContentCachingRequestWrapper works, but it can only log the request body AFTER the request was sent.
    // So we need to apply some stronger magic on Dumbledore Level


		// Log REQUEST body (for PUT and POST requests)
		BufferedRequestWrapper wrappedRequest = new BufferedRequestWrapper(request);
		if (logger.isDebugEnabled()) {
			if (wrappedRequest.getBufferedContent().length > 0) {
				String requestBody = this.getContentAsString(wrappedRequest.getBufferedContent(), this.maxPayloadLength, request.getCharacterEncoding());
				if (requestBody.indexOf("\n") > 0) {
					this.logger.debug("   Request body:\n" + requestBody);
				} else {
					this.logger.debug("   " + requestBody);
				}
			} else {
				if (HttpMethod.POST.matches(request.getMethod()) || HttpMethod.PUT.matches(request.getMethod())) {
					this.logger.debug("   " + requestId + " EMPTY body in "+request.getMethod());
				}
			}
		}

		// Also wrap the response, so that we can log the response payload and the payload is still available
		ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

		// ======== perform the actual HTTP request ==========
    filterChain.doFilter(wrappedRequest, wrappedResponse);
		// ===================================================

		// Log RESPONSE body
    long duration = System.currentTimeMillis() - startTime;
    this.logger.debug("<= " + reqInfo + " returned " + response.getStatus() + " in "+duration + "ms.");
    if (includeResponsePayload && wrappedResponse.getContentSize() > 0) {
      byte[] buf = wrappedResponse.getContentAsByteArray();
      String responseStr = getContentAsString(buf, this.maxPayloadLength, response.getCharacterEncoding());
      if (responseStr.indexOf("\n") > 0) {
	      this.logger.debug("   server's response body:\n"+responseStr);
      } else {
	      this.logger.debug("   server's response body: "+responseStr);
      }
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
