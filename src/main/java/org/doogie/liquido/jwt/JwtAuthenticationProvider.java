package org.doogie.liquido.jwt;

import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Validate the authentication of a user with the credentials provided in the JWT
 * UserID must exist in the DB.
 */
@Slf4j
@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

	@Autowired
	UserRepo userRepo;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		LiquidoAuthentication auth = (LiquidoAuthentication)authentication;
		Long userId = auth.getUserId();
		UserModel user = userRepo.findById(userId)
			.orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Cannot find and authenticate from JWT user.id="+userId));
		//TODO: sanity check user. isExpired, is locked, is in team etc.
		auth.setAuthenticated(true);
		auth.setDetails(user);
		log.debug("Successfully authenticated "+user.toStringShort());
		return authentication;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return LiquidoAuthentication.class.isAssignableFrom(authentication);
	}
}
