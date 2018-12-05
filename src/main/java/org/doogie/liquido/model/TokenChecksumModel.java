package org.doogie.liquido.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

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
@Table(name = "checksums", uniqueConstraints= {
	@UniqueConstraint(columnNames = {"area_id", "public_proxy_id"})  // A proxy cannot be public proxy more than once in one area.
})
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
	LocalDateTime expiresAt;

	/**
	 * A voter can delegate his right to vote to a proxy.
	 * This voter may be a proxy himself. Then he delegates his own vote and all the votes delegated to him
	 * to his parent proxy. This way a tree of proxies is formed.
	 */
	@ManyToOne
	//@JoinColumn(name = "delegatedToProxy")
	TokenChecksumModel delegatedTo;

	/**
	 * A voter can delegate his vote to a proxy.
	 * But maybe the voter does not want that his proxy in turn delegates the vote again.
	 * Then the delegation is marked as nonTransitive.
	 * By default delegations can be transitive, so that a tree of delegations can be formed.
 	 */
	boolean transitive = true;

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

	/**
	 * When a voter wants to delegate his vote to a proxy, but that proxy is not a public proxy,
	 * then the delegated is requested until the proxy accepts it.
	 */
  @OneToOne
	UserModel requestedDelegationTo;

  /** When was the delegation to that proxy requested */
  LocalDateTime requestedDelegationAt;


	//There is deliberately no createdBy in this class
	//For the same reason there is also no createdAt or updatedAt. They might lead to timing attacks.


}
