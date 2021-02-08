package org.doogie.liquido.test.testUtils;

import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.jwt.LiquidoAuthentication;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
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

	@Override
	public SecurityContext createSecurityContext(WithMockTeamUser withMockTeamUser) {
		//Compare to: authUtil.authenticateInSecurityContext(withMockTeamUser.userId(), withMockTeamUser.teamId());
		SecurityContext context = SecurityContextHolder.createEmptyContext();


		Long userId;
		if (withMockTeamUser.email() != "") {
			UserModel userModel = userRepo.findByEmail(withMockTeamUser.email())
				.orElseThrow(() -> new RuntimeException("Cannot create mock user. No user with email=" + withMockTeamUser.email()));
			userId = userModel.getId();
		} else {
			userId = withMockTeamUser.userId();
		}
		Long teamId = withMockTeamUser.teamId();
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority(ROLE_USER));
		Optional<TeamModel> teamOpt = teamRepo.findByIdAndAdminsIdEquals(teamId, userId);
		if (teamOpt.isPresent()) {
			authorities.add(new SimpleGrantedAuthority(ROLE_TEAM_ADMIN));
		}
		LiquidoAuthentication liquidoAuth = new LiquidoAuthentication(userId, teamId, authorities);
		context.setAuthentication(liquidoAuth);
		return context;
	}

}
