package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ChecksumModel {

	/**
	 * A checksum validates a voter token.
	 * checksumModel = hash(voterToken)
	 * The checksum field is also the ID of this entity.  ("Fachlicher Schlüssel")
	 */
	@Id
	@NonNull
	String checksum;

	/**
	 * The area is actually already encoded in the voterToken.
	 * So that means that the area in this ChecksumModel must correspond to the area of the voterToken
	 */
	@OneToOne
	@NonNull
	AreaModel area;

	/** Checksums are only valid for a given time */
	LocalDateTime expiresAt;

	/**
	 * A voter can delegate his right to vote to a proxy. This delegation is stored in the DelegationModel.
	 * Here we store the completely anonymous delegation from one checksum to another one.
	 * There is no relation between a checksum and a voter (except for public proxies).
	 */
	@ManyToOne
	//@JoinColumn(name = "delegatedToProxy")
	@JsonIgnore    // do not reveal if a checksum is delegated
	ChecksumModel delegatedTo;

	/* List of checksums that are delegated to this as a proxy. Inverse of bidirectional delegatedToProxy association
	@OneToMany(mappedBy = "proxyFor", fetch = FetchType.EAGER)
	List<ChecksumModel> proxyFor;
  */

	/**
	 * A voter can delegate his vote to a proxy.
	 * But maybe the voter does not want that his proxy in turn delegates the vote again.
	 * Then the delegation is marked as nonTransitive.
	 * By default delegations can be transitive, so that a tree of delegations can be formed.
	 */
	boolean transitive = true;

	/**
	 * If a user want's to be a public proxy, then he CAN store his user together with his checksum.
	 * Then voters can automatically delegate their vote to this proxy.
	 */
	@OneToOne
	UserModel publicProxy = null;		// by default no username is stored together with a checksum!!!


	//REFACTORED: I decided to store delegation requests in the DelegationModel
	/*
	 * When a voter wants to delegate his vote to a proxy, but that proxy is not a public proxy,
	 * then the delegated is requested until the proxy accepts it.

  @OneToOne
	UserModel requestedDelegationTo;

  /* When was the delegation to that proxy requested
  LocalDateTime requestedDelegationAt;

	*/

	//There is deliberately no createdBy in this class
	//For the same reason there is also no createdAt or updatedAt. They might lead to timing attacks.


}
