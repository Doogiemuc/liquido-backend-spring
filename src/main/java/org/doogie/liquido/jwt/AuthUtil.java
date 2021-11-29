package org.doogie.liquido.jwt;

import lombok.NonNull;
import org.doogie.liquido.datarepos.TeamRepo;
import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.TeamModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.security.LiquidoAuditorAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Utilities for working with a User authenticated via JWT.
 *
 * The {@link LiquidoAuthentication} token only contains userId, teamId and JWT.
 * With these utility methods you can fetch the full UserModel and TeamModel.
 */
@Service
public class AuthUtil {

	public static final String ROLE_USER = "ROLE_USER";
	public static final String HAS_ROLE_USER = "hasRole('" + ROLE_USER + "')";
	public static final String ROLE_TEAM_ADMIN = "ROLE_TEAM_ADMIN";
	public static final String HAS_ROLE_TEAM_ADMIN = "hasRole('" + ROLE_TEAM_ADMIN +"')";
	public static final String tokenRequestHeader = "Authorization";
	public static final String tokenRequestHeaderPrefix = "Bearer ";   // with trailing space! and e before a !!! :-)

	@Autowired
	UserRepo userRepo;

	@Autowired
	TeamRepo teamRepo;

	/**
	 * Put our own {@link LiquidoAuthentication} into spring's SecurityContext.
	 * Login a user into a team. This will check if user is an admin with the DB
	 * and add the corresponding SimpleGrantedAuthority.
	 *
	 * <b>This is called for each and every request from {@link JwtAuthenticationFilter} So keep this method fast !!!</b>
	 *
	 * @param userId user id
	 * @param teamId team id
	 * @param jwt already validated JWT (is validated in {@link JwtAuthenticationFilter})
	 * @return the SecurityContext with the LiquidoAuthentication object in it.
	 */
	public SecurityContext authenticateInSecurityContext(@NonNull Long userId, Long teamId, String jwt) {
		//TODO: make teamId @NonNull down here!
		Set<GrantedAuthority> authorities = new HashSet<>();
		authorities.add(new SimpleGrantedAuthority(ROLE_USER));
		//TODO: should the admin role be encoded in the JWT? This would safe the DB request
		if (userIsAdminInTeam(userId, teamId))
			authorities.add(new SimpleGrantedAuthority(ROLE_TEAM_ADMIN));
		LiquidoAuthentication liquidoAuth = new LiquidoAuthentication(userId, teamId, jwt, authorities);
		SecurityContextHolder.getContext().setAuthentication(liquidoAuth);
		return SecurityContextHolder.getContext();
	}

	/**
	 * This server is stateless. So there is no login or logout functionality.
	 * But while a request is processed, there is an {@link Authentication} in Spring-Security.
	 * With this method, that Authentication can be removed. Mainly used by tests.
	 */
	public void logoutOfSecurityContext() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	/**
	 * Get the LiquidoAuthentication object from the Security Context. Our {@link JwtAuthenticationFilter} has put it in there.
	 * @return (optional) LiquidoAuthentication if authenticated.
	 */
	public Optional<LiquidoAuthentication> getLiquidoAuthentication() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null ||
			auth instanceof AnonymousAuthenticationToken ||
			(auth.getPrincipal() != null && auth.getPrincipal().equals("anonymousUser")) // see AnonymousConfigurer
		) {
			return Optional.empty();
		}
		LiquidoAuthentication liquidoAuth = (LiquidoAuthentication) auth;
		return Optional.of(liquidoAuth);
	}

	/**
	 * Check if a given user is the admin of this team. <b>This will fire a DB request!</b>.
	 * Alternatively you can call {@link TeamModel#isAdmin(UserModel)}
	 * @param teamId Id of an existing team
	 * @param userId an existing userId in that team
	 * @return true, if user and team exist, and user is an admin in that team.
	 */
	public boolean userIsAdminInTeam(Long userId, Long teamId) {
		Optional<TeamModel> teamOpt = teamRepo.findByIdAndAdminsIdEquals(teamId, userId);
		return teamOpt.isPresent();
	}

	public boolean isCurrentUserAdminInTeam() {
		Optional<LiquidoAuthentication> liquidoAuthentication = getLiquidoAuthentication();
		if (!liquidoAuthentication.isPresent()) return false;
		return userIsAdminInTeam(liquidoAuthentication.get().userId, liquidoAuthentication.get().getTeamId());
	}

	/**
	 * Get the currently logged in user from the DB.
	 * <b>This calls the DB!</b>
	 * @return UserModel or <b>Optional.empty()</b>
	 *   if this is an anonymous request
	 *   ie. no authentication info was sent with a JWT
	 *   or if the user.id couldn't be found in the DB
	 */
	public Optional<UserModel> getCurrentUserFromDB()  {
		Optional<LiquidoAuthentication> liquidoAuth = this.getLiquidoAuthentication();
		if (!liquidoAuth.isPresent()) return Optional.empty();
		liquidoAuth.get().getDetails();
		return userRepo.findById(liquidoAuth.get().getUserId());   // may return Optional.empty if user.id is not found in DB!
	}

	/**
	 * Get detailed info about the team that the user is currently logged in.
	 * One user may be an admin or member in several teams. But he is always logged into one specific team.
	 * <b>This calls the DB!</b>
	 * @return TeamModel or <b>null</b> if no one is logged in
	 */
	public Optional<TeamModel> getCurrentTeamFromDB() {
		Optional<LiquidoAuthentication> liquidoAuth = this.getLiquidoAuthentication();
		if (!liquidoAuth.isPresent()) return Optional.empty();
		return teamRepo.findById(liquidoAuth.get().getTeamId());
	}

	public Optional<String> getCurrentJwt() {
		Optional<LiquidoAuthentication> liquidoAuth = this.getLiquidoAuthentication();
		if (!liquidoAuth.isPresent()) return Optional.empty();
		return Optional.of(liquidoAuth.get().getJwt());
	}

	/**
	 * Extract the token from the Authorization request header (if there is any)
	 * @return the JWT or null if there was no "Authorization: Bearer ..." in the request header
	 * @param request
	 */
	public String getJwtFromRequest(HttpServletRequest request) {
		//FUN FACT: in the Micronaut framework, this same logic is implemented in five classes :-)
		String bearerToken = request.getHeader(tokenRequestHeader);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(tokenRequestHeaderPrefix)) {
			return bearerToken.replace(tokenRequestHeaderPrefix, "");
		}
		return null;
	}
}
