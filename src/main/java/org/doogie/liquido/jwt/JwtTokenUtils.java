package org.doogie.liquido.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

/**
 * Utility class to generate and then validate JsonWebTokens for Liquido.
 * Each JWT contains the user's <b>ID</b> as JWT "subject" claim.
 */
@Slf4j
@Component
public class JwtTokenUtils {

	@Value("${liquido.jwt.secret}")
	private String jwtSecret;

	@Value("${liquido.jwt.expiration}")
	private Long jwtExpirationInMs;

	public static final String TEAM_ID_CLAIM = "teamId";

	/**
	 * This generates a new JWT. This needs jwtSecret as input, so that only the server can
	 * generate JWTs.
	 */
	public String generateToken(@NonNull String userId, String teamId) {
		Instant expiryDate = Instant.now().plusMillis(jwtExpirationInMs);
		//TODO: for now teamId may be null for users that are not part of a team (for web client)
		return Jwts.builder()
				.setSubject(userId)
				.claim(TEAM_ID_CLAIM, teamId)
				//.setClaims(claims)   //BUGFIX: This overwrites all other claims!!!
				.setIssuedAt(Date.from(Instant.now()))
				.setExpiration(Date.from(expiryDate))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}

	public String generateToken(@NonNull Long userId, Long teamId) {
		return this.generateToken(userId.toString(), teamId != null ? teamId.toString() : null);
	}

	/**
	 * Returns the user id encapsulated within the token
	 */
	public Long getUserIdFromJWT(String token) {
		Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
		return Long.valueOf(claims.getSubject());
	}

	/**
	 * Get the team id from the given token
	 * @param token a JWT
	 * @return teamID that is encoded in the token
	 */
	public Long getTeamIdFromJWT(String token) {
		Claims claims = Jwts.parser()
			.setSigningKey(jwtSecret)
			.parseClaimsJws(token)
			.getBody();
		String teamId = (String) claims.get(TEAM_ID_CLAIM);
		if (teamId == null) return null;   //TODO: make teamId required
		return Long.valueOf(teamId);
	}

	/**
	 * Validates if a token has the correct unmalformed signature and is not expired or unsupported.
	 * @throws LiquidoException when token is invalid.
	 */
	public boolean validateToken(String authToken) throws LiquidoException {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
			return true;
		} catch (SignatureException ex) {
			log.debug("Invalid JWT signature");
			throw new LiquidoException(LiquidoException.Errors.JWT_TOKEN_INVALID, "Incorrect signature");
		} catch (MalformedJwtException ex) {
			log.debug("Invalid JWT token");
			throw new LiquidoException(LiquidoException.Errors.JWT_TOKEN_INVALID, "Malformed jwt token");
		} catch (ExpiredJwtException ex) {
			log.debug("Expired JWT token");
			throw new LiquidoException(LiquidoException.Errors.JWT_TOKEN_EXPIRED, "Token expired. Refresh required.");
		} catch (UnsupportedJwtException ex) {
			log.debug("Unsupported JWT token");
			throw new LiquidoException(LiquidoException.Errors.JWT_TOKEN_INVALID, "Unsupported JWT token");
		} catch (IllegalArgumentException ex) {
			log.debug("JWT claims string is empty.");
			throw new LiquidoException(LiquidoException.Errors.JWT_TOKEN_INVALID, "Illegal argument token");
		}
	}

	/**
	 * Return the jwt expiration for the client so that they can execute
	 * the refresh token logic appropriately
	 */
	public Long getExpiryDuration() {
		return jwtExpirationInMs;
	}


}