package org.doogie.liquido.jwt;

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

	@Value("${app.jwt.secret}")   //TODO: how to make this jwtSecret more "secret" ?
	private String jwtSecret;

	@Value("${app.jwt.expiration}")
	private Long jwtExpirationInMs;

	@Value("${app.jwt.claims.refresh.name}")
	private String jwtClaimRefreshName;

	/**
	 * This generates a new JWT. This needs jwtSecret as input, so that only the server can
	 * generate JWTs.
	 */
	public String generateToken(String subject) {
		Instant expiryDate = Instant.now().plusMillis(jwtExpirationInMs);
		return Jwts.builder()
				.setSubject(subject)
				.setIssuedAt(Date.from(Instant.now()))
				.setExpiration(Date.from(expiryDate))
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}

	/**
	 * Returns the user id encapsulated within the token
	 */
	public String getSubjectFromJWT(String token) {
		Claims claims = Jwts.parser()
				.setSigningKey(jwtSecret)
				.parseClaimsJws(token)
				.getBody();
		return claims.getSubject();
	}

	/**
	 * Validates if a token has the correct unmalformed signature and is not expired or unsupported.
	 */
	public boolean validateToken(String authToken) throws LiquidoException {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
			return true;
		} catch (SignatureException ex) {
			log.error("Invalid JWT signature");
			throw new LiquidoException(LiquidoException.Errors.INVALID_JWT_TOKEN, "Incorrect signature");
		} catch (MalformedJwtException ex) {
			log.error("Invalid JWT token");
			throw new LiquidoException(LiquidoException.Errors.INVALID_JWT_TOKEN, "Malformed jwt token");
		} catch (ExpiredJwtException ex) {
			log.error("Expired JWT token");
			throw new LiquidoException(LiquidoException.Errors.INVALID_JWT_TOKEN, "Token expired. Refresh required.");
		} catch (UnsupportedJwtException ex) {
			log.error("Unsupported JWT token");
			throw new LiquidoException(LiquidoException.Errors.INVALID_JWT_TOKEN, "Unsupported JWT token");
		} catch (IllegalArgumentException ex) {
			log.error("JWT claims string is empty.");
			throw new LiquidoException(LiquidoException.Errors.INVALID_JWT_TOKEN, "Illegal argument token");
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