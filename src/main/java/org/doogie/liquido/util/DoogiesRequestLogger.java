package org.doogie.liquido.util;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Doogies very cool HTTP request logging
 *
 * see also {@link org.springframework.web.filter.CommonsRequestLoggingFilter}
 *
 * https://mdeinum.wordpress.com/2015/07/01/spring-framework-hidden-gems/
 */
public class DoogiesRequestLogger extends OncePerRequestFilter {

  /**
   * Log each request with ReqID, full Request URI and its duration in ms.
   * @param req the request
   * @param res the response
   * @param filterChain chain of filteres
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
    long startTime = System.currentTimeMillis();
    String reqId = "[" + startTime % 10000 + "]";
    this.logger.debug("=> " + reqId + " " + req.getMethod() + " " + req.getRequestURI());
    /*
      //Keep in mind, that we cannon simply log the body. Because this input stream can only be read once.
      String requestBody = DoogiesUtil._stream2String(req.getInputStream());
      this.logger.trace("   " + requestBody);
    */
    filterChain.doFilter(req, res);
    long duration = System.currentTimeMillis() - startTime;
    this.logger.debug("<= " + reqId + " " + req.getMethod() + " " + req.getRequestURI() + ": returned status=" + res.getStatus() + " in "+duration + "ms");
  }

}
