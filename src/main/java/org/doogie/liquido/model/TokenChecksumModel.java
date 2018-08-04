package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * A token is the digital anonymous right to vote. The checksum represents the right to vote in this area.
 *
 * When a voter requests a token, then the server calculates two values:
 * 1. voterToken = hash(user.email, user.passwordHash, area.id)
 * 2. checksum   = hash(voterToken, secretSeed)
 *
 * Only the checksum is (anonomously) stored on the server. And
 * only the user knows the voterToken that the checksum is created from.
 *
 * When a user wants to cast a vote, then he sends his voterToken for this area.
 * Then the server checks if he already knows the corresponding checksum.
 * If yes, then the casted vote is valid and will be counted.
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
public class TokenChecksumModel {
	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	public Long id;

	@NotNull
	@NonNull
	@Column(unique = true)
	String checksum;

	//There is deliberately no createdBy here! Tokens must not be related to any user. Tokens must be anonymous!
	//For the same reason there is also no createdBy or updatedBy. They might lead to timing attacks.

	// I thought about storing the area next to the checksum. But the area is already encoded inside this hash value.
}
