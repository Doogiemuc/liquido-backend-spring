package org.doogie.liquido.jwt;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.doogie.liquido.jwt.AuthUtil.ROLE_USER;

/**
 * This filter can authenticate requests from the passed JWT in the HTTP header "Authentication: Bearer [jwt]".
 * It is added before the default spring UsernamePasswordAuthenticationFilter.class
 * The JWT contains the userId as jwt "subject" and also the teamId as a JWT "claim".
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final String tokenRequestHeader = "Authorization";

	private final String tokenRequestHeaderPrefix = "Bearer ";   // with trailing space! and e before a !!! :-)

	@Autowired
	JwtTokenUtils jwtTokenUtils;

	@Autowired
	AuthUtil authUtil;

	/**
	 * Filter the incoming request for a valid token in the request header
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
																	FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwt = getJwtFromRequest(request);
			if (StringUtils.hasText(jwt) && jwtTokenUtils.validateToken(jwt)) {
				Long userId = jwtTokenUtils.getUserIdFromJWT(jwt);
				Long teamId = jwtTokenUtils.getTeamIdFromJWT(jwt);
				authUtil.authenticateInSecurityContext(userId, teamId);   // this will fire a DB request for user's roles
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

	/**
	 * Extract the token from the Authorization request header (if there is any)
	 * @return the JWT or null if there was no "Authorization: Bearer ..." in the request header
	 */
	private String getJwtFromRequest(HttpServletRequest request) {
		//FUN FACT: in the Micronaut framework, this same logic is implemented in five classes :-)
		String bearerToken = request.getHeader(tokenRequestHeader);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenRequestHeaderPrefix)) {
			//log.info("Extracted Token: " + bearerToken);
			return bearerToken.replace(tokenRequestHeaderPrefix, "");
		}
		return null;
	}
}
