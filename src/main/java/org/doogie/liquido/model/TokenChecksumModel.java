package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.*;

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

	/**
	 * The area is actually already encoded in the voterToken.
	 * So that means that the area in this TokenChecksumModel must correspond to the area of the voterToken
	 */
	@OneToOne
	@NonNull
	AreaModel area;

	/** TODO: Checksums are only valid for a given time */
	//Date expiresAt;

	/**
	 * A voter can delegate his right to vote to a proxy.
	 * This voter may be a proxy himself. Then he delegates his own vote and all the votes delegated to him
	 * to his parent proxy. This way a tree of proxies is formed.
	 */
	@ManyToOne
	//@JoinColumn(name = "delegatedToProxy")
	TokenChecksumModel delegatedTo;

	/* List of checksums that are delegated to this as a proxy. Inverse of bidirectional delegatedToProxy association
	@OneToMany(mappedBy = "proxyFor", fetch = FetchType.EAGER)
	List<TokenChecksumModel> proxyFor;
  */

	/**
	 * If a user want's to be a public proxy, then he CAN store his user together with his checksum.
	 * Then voters can automatically delegate their vote to this proxy.
	 */
	@OneToOne
	UserModel publicProxy = null;		// by default no username is stored together with a checksum!!!


	//There is deliberately no createdBy in this class
	//For the same reason there is also no createdAt or updatedAt. They might lead to timing attacks.


}
