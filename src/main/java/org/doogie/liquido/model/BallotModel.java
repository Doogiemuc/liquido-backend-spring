package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.doogie.liquido.rest.converters.PollAsLinkJsonSerializer;
import org.doogie.liquido.testdata.LiquidoProperties;
import org.springframework.security.crypto.bcrypt.BCrypt;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * POJO Entity that represents an anonymous vote that a user has casted for one given poll.
 *
 * Each ballot contains the ordered list of proposals that this user voted for.
 * But the ballot does *NOT* contain any reference to the voter.
 * Instead each ballot contains a checksum which is the hashed value of the user's voterToken.
 *
 * Only the voter knows his own voterToken. So only he can check that this actually is his ballot.
 * This way a voter can even update his ballot as long as the voting phase is still open.
 *
 * BallotRepo is not exposed as REST resource. BallotModel is serialized to JSON manually. See {@link PollAsLinkJsonSerializer}
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor  //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@Table(name = "ballots", uniqueConstraints= {
  @UniqueConstraint(columnNames = {"POLL_ID", "hashedVoterToken"} )   // a voter is only allowed to vote once per poll with his hashedVoterToken!
})
public class BallotModel {
	//BallotModel deliberately does NOT extend BaseModel!
	//No @CreatedDate, No @LastModifiedDate !  This could lead to timing attacks.
	//No @CreatedBy ! When voting it is confidential who casted this ballot and when.

	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	public Long id;

  /**
	 * Reference to the poll this this ballot was casted in.
	 */
  @NotNull
  @NonNull
  @ManyToOne
	@JsonProperty("_links")   											// JSON will contain "_links.poll.href"
	@JsonSerialize(using = PollAsLinkJsonSerializer.class)
  public PollModel poll;

	/**
	 * Get current status of poll that this ballot was casted in.
	 * This will expose the PollStatus in the JSON response.
	 * If the poll is still in its voting phase (poll.status == VOTING),
	 * then the ballot can still be changed by the voter.
	 * @return PollStatus, e.g. VOTING or FINISHED
	 */
  public PollModel.PollStatus getPollStatus() {
  	return this.poll != null ? poll.getStatus() : null;
	}

  /**
	 * level = 0: user voted for himself
	 * level = 1: direct proxy
	 * level = 2: transitive proxy voted
	 * etc. */
  @NonNull   // level must be set in RequiredArgsConstructor
	@NotNull
  public Integer level;

	/*
	 * Number of times that his vote was counted because of delegations.
	 * This value is not stored, since it may change when the tree of delegations changes.
	 * It is only valid at the time when the ballot is casted.
	@Transient
  public Long voteCount;
	*/

  /**
   * One vote puts some proposals of this poll into his personally preferred order.
   * One voter may putArray some or all proposals of the poll into his (ordered) ballot. But of course he may only vote at maximum once for every proposal.
   * And one proposal may be voted for by several voters => ManyToMany relationship
   */
  //BE CAREFULL: Lists are not easy to handle in hibernate: https://vladmihalcea.com/hibernate-facts-favoring-sets-vs-bags/
  @NonNull
  @NotNull
  @ManyToMany(fetch = FetchType.EAGER)   //(cascade = CascadeType.MERGE, orphanRemoval = false)
  @OrderColumn(name="LawModel_Order")  // keep order in DB
  public List<LawModel> voteOrder;   		//proposals in voteOrder must not be duplicate! This is checked in VoteRestController.

	public void setVoteOrder(List<LawModel> voteOrder) {
		if (voteOrder == null || voteOrder.size() == 0)
			throw new IllegalArgumentException("Vote Order must not be null or empty!");
		this.voteOrder = voteOrder;
	}

  /**
   * Encrypted and anonymous information about the voter that casted this vote into the ballot.
	 * Only the voter knows the voterToken that this checksumModel was created from as
	 *   rightToVote.hashedVoterToken = hash(voterToken)
	 * If a vote was casted by a proxy, this is still the voters (delegated) checksum.
   */
  @NotNull
  @NonNull
	@OneToOne
	@JoinColumn(name = "hashedVoterToken")		// The @Id of a RightToVoteModel is the hashedVoterToken itself
	@JsonIgnore																// [SECURITY] Do not expose voter's private right to vote (which might also include public proxies name)
  public RightToVoteModel rightToVote;

	/**
	 * The MD5 checksum of a ballot uniquely identifies this ballot.
	 * The checksum is calculated from the voteOrder, poll.id and rightToVote.hashedVoterToken.
	 * It deliberately does not depend on level or rightToVote.delegatedTo !
	 */
	public String checksum;

	@PreUpdate
	@PrePersist
	public void calcMD5Checksum() {
		this.checksum =  DigestUtils.md5Hex(
				// Cannot include ID. Its not not present when saving a new Ballot!
				this.getVoteOrder().hashCode() +
				this.getPoll().hashCode() +
				this.getRightToVote().hashedVoterToken);
	}

	public AreaModel getArea() {
		return this.getPoll() != null ? this.getPoll().getArea() : null;
	}

	@Override
	public String toString() {
		String proposalIds = voteOrder.stream().map(law->law.getId().toString()).collect(Collectors.joining(","));
		return "BallotModel{" +
				"id=" + id +
				", poll(id=" + poll.getId() +
			  ", status="+poll.getStatus() + ")" +
				", level=" + level +
				", voteOrder(proposalIds)=[" + proposalIds +"]"+
				//Do not expose rightToVote", rightToVote="+ rightToVote +    which might include public proxy name
				"}";
	}
}
