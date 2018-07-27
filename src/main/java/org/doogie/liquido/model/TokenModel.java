package org.doogie.liquido.model;

import lombok.Data;
import lombok.NonNull;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * A token is the digital anonymous right to vote. The areaToken represents the right to vote in this area.
 *
 * When a voter requests a token,
 * then the server calculates two values:
 * 1. voterToken = hash(user.email, user.passwordHash, area.id)
 * 2. areaToken  = hash(voterToken, secretSeed)
 *
 * Only the areaToken is (anonomously) stored on the server.
 * Now only the user knows the voterToken. Only he could create it because only the user knows his password.
 *
 * When a user wants to cast a vote, then he sends his voterToken for this area.
 * Then the server checks if the already knows the corresponding areaToken.
 * If yes, then the casted vote is valid and will be counted.
 */
@Data
@Entity
public class TokenModel {
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	public Long id;

	@NotNull
	@NonNull
	@Column(unique = true)
	String areaToken;

	//There is deliberately no createdBy here! Tokens must not be related to any user. Tokens must be anonymous!
	//For the same reason there is also no createdBy or updatedBy. They might lead to timing attacks.
}
