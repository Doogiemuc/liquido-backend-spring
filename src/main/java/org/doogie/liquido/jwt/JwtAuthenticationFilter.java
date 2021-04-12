package org.doogie.liquido.jwt;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter can authenticate requests from the passed JWT in the HTTP header "Authentication: Bearer [jwt]".
 * It is added before the default spring UsernamePasswordAuthenticationFilter.class
 * The JWT contains the userId as jwt "subject" and also the teamId as a JWT "claim".
 *
 * When the request does not have any JWT in its header, then we forward to the next filter
 * If the JWT is expired then we throw a LiquidoException with errorCode JWT_TOKEN_EXPIRED.
 * If the JWT is valid, then we extract the userId and TeamId from it and authenticate that user
 * in spring's SecurityContext.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	@Autowired
	JwtTokenUtils jwtTokenUtils;

	@Autowired
	AuthUtil authUtil;

	/**
	 * Filter the incoming request for a valid token in the request header.
	 * <b>This method is called for every request, so this needs to be quick!</b>
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
																	FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwt = authUtil.getJwtFromRequest(request);
			if (StringUtils.hasText(jwt) && jwtTokenUtils.validateToken(jwt)) {
				Long userId = jwtTokenUtils.getUserIdFromJWT(jwt);
				Long teamId = jwtTokenUtils.getTeamIdFromJWT(jwt);
				authUtil.authenticateInSecurityContext(userId, teamId, jwt);   // this will fire a DB request for user's roles
			}

			filterChain.doFilter(request, response);		// IMPORTANT: MUST always continue chain of filters

		} catch (LiquidoException lex) {  // thrown when token is invalid
			//BUGFIX: https://stackoverflow.com/questions/19767267/handle-spring-security-authentication-exceptions-with-exceptionhandler
			//https://stackoverflow.com/questions/34595605/how-to-manage-exceptions-thrown-in-filters-in-spring/34633687#34633687
			//This does not really work: response.sendError(e.getHttpResponseStatus().value(), "Error msg from JwtAuthenticationFilter: "+ e.getMessage());
			log.debug(lex.toString());
			response.setStatus(lex.getHttpResponseStatus().value());
			response.setContentType("application/json");
			Lson lson = lex.toLson();
			lson.put("requestURL", request.getRequestURL().toString());
			response.getWriter().println(lson.toString());
		}
	}

}
