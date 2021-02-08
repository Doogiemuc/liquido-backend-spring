package org.doogie.liquido.jwt;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * This token identifies a logged in user.
 * It is created from the passed JWT in {@link JwtAuthenticationFilter}
 * and then later checked in {@link JwtAuthenticationProvider}
 */
public class LiquidoAuthentication extends AbstractAuthenticationToken {

	Long userId;
	Long teamId;

	public LiquidoAuthentication(@NonNull Long userId, Long teamId, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.userId = userId;
		this.teamId = teamId;
	}

	@Override
	public Object getCredentials() {
		//TODO: Should I return the jwt here?
		return userId;
	}

	/**
	 * @return UserId of principal
	 */
	@Override
	public Object getPrincipal() {
		return this.userId;
	}

	public Long getUserId() { return this.userId; }

	public Long getTeamId() { return teamId; }
}
