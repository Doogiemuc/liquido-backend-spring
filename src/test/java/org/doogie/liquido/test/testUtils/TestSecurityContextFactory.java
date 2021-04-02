package org.doogie.liquido.test.testUtils;

import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.AuthUtil;
import org.doogie.liquido.jwt.JwtTokenUtils;
import org.doogie.liquido.jwt.LiquidoAuthentication;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.util.DoogiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.doogie.liquido.jwt.AuthUtil.ROLE_TEAM_ADMIN;
import static org.doogie.liquido.jwt.AuthUtil.ROLE_USER;

/**
 * This factory creates a mock SecurityContext that can be used in tests.
 * @See WithMockTeamUser annotation.
 */
public class TestSecurityContextFactory implements WithSecurityContextFactory<WithMockTeamUser> {

	@Autowired
	UserRepo userRepo;

	@Autowired
	TeamRepo teamRepo;

	@Autowired
	AuthUtil authUtil;

	@Autowired
	JwtTokenUtils jwtTokenUtils;

	/**
	 * Create an authenticated mock user in SecurityContext.
	 *
	 * @param mockUserAnnotation the {@link WithMockTeamUser} annotation
	 * @return the SecurityContext
	 */
	@Override
	public SecurityContext createSecurityContext(WithMockTeamUser mockUserAnnotation) {
		Long userId = mockUserAnnotation.userId();
		if (userId == -1) {
			if ("".equals(mockUserAnnotation.email())) {
				throw new RuntimeException("Need either userId or email to authenticate a mock user");
			} else {
				userId = userRepo.findByEmail(mockUserAnnotation.email())
					.orElseThrow(() -> new RuntimeException("Cannot create mock user. No user with email=" + mockUserAnnotation.email()))
					.getId();
			}
		}
		String jwt = jwtTokenUtils.generateToken(userId, mockUserAnnotation.teamId());
		return authUtil.authenticateInSecurityContext(userId, mockUserAnnotation.teamId(), jwt);
	}

}
