package org.doogie.liquido.jwt;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.rest.LiquidoRestExceptionHandler;
import org.doogie.liquido.security.LiquidoUserDetailsService;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This filter is added before the default spring UsernamePasswordAuthenticationFilter.class
 * It can authenticate user's from the header "Authentication: Bearer [jwt]".
 * The JWT contains the user's email. This will be used to load the UserDetails.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final String tokenRequestHeader = "Authorization";

	private final String tokenRequestHeaderPrefix = "Bearer ";   // with trailing space!

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private LiquidoUserDetailsService liquidoUserDetailsService;

	/**
	 * Filter the incoming request for a valid token in the request header
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
																	FilterChain filterChain) throws ServletException, IOException {
		try {
			String jwt = getJwtFromRequest(request);
			if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
				//log.trace("Trying to authenticate JWT: "+jwt);
				String email = jwtTokenProvider.getSubjectFromJWT(jwt);   // jwt subject is user's email
				UserDetails userDetails = liquidoUserDetailsService.loadUserByUsername(email);
				//----- if token is valid and user was found, then login that user as principal.
				//TODO: Create a   class LiquidoAuthenticationToken extends AbstractAuthenticationToken {  }  and put info abuot user and team into it

				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());  // <==== no password. User is authenticated by JWT
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}

			filterChain.doFilter(request, response);		// IMPORTANT: MUST always continue chain of filters

		} catch (UsernameNotFoundException e) {
			log.debug("Authenticate JWT: Username from JWT does not exist", e);
			throw e;
		} catch (LiquidoException lex) {  // thrown when token is invalid
			//BUGFIX: https://stackoverflow.com/questions/19767267/handle-spring-security-authentication-exceptions-with-exceptionhandler
			log.debug(lex.toString());
			response.setStatus(lex.getHttpResponseStatus().value());
			response.setContentType("application/json");
			Lson lson = lex.toLson();
			lson.put("requestURL", request.getRequestURL().toString());
			response.getWriter().println(lson.toString());
		}
	}

	//https://stackoverflow.com/questions/34595605/how-to-manage-exceptions-thrown-in-filters-in-spring/34633687#34633687
	//This does not really work: response.sendError(e.getHttpResponseStatus().value(), "Error msg from JwtAuthenticationFilter: "+ e.getMessage());


	/**
	 * Extract the token from the Authorization request header
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
