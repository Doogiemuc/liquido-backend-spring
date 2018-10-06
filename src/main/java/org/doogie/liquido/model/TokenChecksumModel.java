package org.doogie.liquido.model;

import jdk.nashorn.internal.parser.Token;
import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * When a voter requests a voterToken for an area , then the server calculates two values:
 * 1. voterToken = hash(user.email, user.passwordHash, area.id)
 * 2. checksumModel = hash(voterToken, secretSeed)
 *
 * Only the checksumModel is (anonomously) stored on the server. And
 * only the user knows the voterToken that the checksumModel is created from.
 *
 * When a user wants to cast a vote, then he sends his voterToken for this area.
 * Then the server checks if he already knows the corresponding checksumModel.
 * If yes, then the casted vote is valid and will be counted.
 */
@Data
@NoArgsConstructor  				// Lombok Data does NOT include a NoArgsConstructor!
@RequiredArgsConstructor		// And RequiredArgsConstructor does not work when not mentioned explicitly
@EqualsAndHashCode(of = "checksum")
@ToString(of = "checksum")
@Entity
@Table(name = "checksums")
public class TokenChecksumModel {

	/** checksumModel = hash(voterToken) */
	@Id
	@NonNull
	String checksum;

	/** TODO: Checksums are only valid for a given time */
	//Date expiresAt;

	/** A voter can delegate his right to vote to a proxy */
	@ManyToOne
	//@JoinColumn(name = "delegatedToProxy")
	TokenChecksumModel delegatedTo;

	/* List of checksums that are delegated to this as a proxy. Inverse of bidirectional delegatedToProxy association
	@OneToMany(mappedBy = "proxyFor", fetch = FetchType.EAGER)
	List<TokenChecksumModel> proxyFor;
  */


	//There is deliberately no createdBy here! Tokens must not be related to any user. Tokens must be anonymous!
	//For the same reason there is also no createdAt or updatedAt. They might lead to timing attacks.

	// I thought about storing the area next to the checksumModel. But the area is already encoded inside this hash value.

}
