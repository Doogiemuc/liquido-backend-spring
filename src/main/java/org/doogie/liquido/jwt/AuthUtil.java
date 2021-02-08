package org.doogie.liquido.jwt;

import lombok.NonNull;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Simple utilities the fetch more details about the currently authenticated user and team.
 * The LiquidoAuthentication token only contains IDs (from the JWT).
 */
@Service
public class AuthUtil {

	public static final String ROLE_USER = "ROLE_USER";
	public static final String HAS_ROLE_USER = "hasRole('" + ROLE_USER + "')";
	public static final String ROLE_TEAM_ADMIN = "ROLE_TEAM_ADMIN";
	public static final String HAS_ROLE_TEAM_ADMIN = "hasRole('" + ROLE_TEAM_ADMIN +"')";

	@Autowired
	UserRepo userRepo;

	@Autowired
	TeamRepo teamRepo;

	/**
	 * Put our own {@link LiquidoAuthentication} into spring's SecurityContext.
	 * Login a user into a team. This will check if user is an admin with the DB
	 * and add the corresponding SimpleGrantedAuthority
	 * @param userId user id
	 * @param teamId team id
	 */
	public void authenticateInSecurityContext(@NonNull Long userId, Long teamId) {
		//TODO: make teamId @NonNull down here!
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority(ROLE_USER));
		if (userIsAdminInTeam(userId, teamId))
			authorities.add(new SimpleGrantedAuthority(ROLE_TEAM_ADMIN));

		LiquidoAuthentication liquidoAuth = new LiquidoAuthentication(userId, teamId, authorities);
		SecurityContextHolder.getContext().setAuthentication(liquidoAuth);
	}

	public boolean userIsAdminInTeam(Long userId, Long teamId) {
		Optional<TeamModel> teamOpt = teamRepo.findByIdAndAdminsIdEquals(teamId, userId);
		return teamOpt.isPresent();
	}

	/**
	 * Get the currently logged in user (if any)
	 * @return UserModel or <b>null</b> if this is an anonymous request
	 */
	public UserModel getCurrentUser()  {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null ||
				auth instanceof AnonymousAuthenticationToken ||
				(auth.getPrincipal() != null && auth.getPrincipal().equals("anonymousUser")) // see AnonymousConfigurer
		) {
			return null;
		}
		LiquidoAuthentication liquidoAuth = (LiquidoAuthentication) auth;
		UserModel user = userRepo.findById(liquidoAuth.getUserId()).orElse(null);
		return user;
	}

	/**
	 * Get detailed info about the team that the user is currently logged in.
	 * One user may be an admin or member in several teams. But he is always logged into one specific team.
	 * @return TeamModel or <b>null</b> if no one is logged in
	 */
	public TeamModel getCurrentTeam() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null ||
			auth instanceof AnonymousAuthenticationToken ||
			(auth.getPrincipal() != null && auth.getPrincipal().equals("anonymousUser")) // see AnonymousConfigurer
		) {
			return null;
		}
		LiquidoAuthentication liquidoAuth = (LiquidoAuthentication) auth;
		TeamModel team = teamRepo.findById(liquidoAuth.getTeamId()).orElse(null);
		return team;
	}
}
