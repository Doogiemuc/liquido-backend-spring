package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * POJO Entity that represents an anonymous vote that a user has casted for one given poll.
 *
 * Each ballot contains the ordered list of proposals that this user voted for.
 * But the ballot does *NOT* contain any reference to the voter.
 * Insted each ballot contains a checksum which is the hashed value of the user's voterToken.
 *
 * Only the voter knows his own voterToken. So only he can check that this actually is his ballot.
 *
 * This way a voter can even update his ballot as long as the voting phase is still open.
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor  //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@Table(name = "ballots", uniqueConstraints= {
  @UniqueConstraint(columnNames = {"POLL_ID", "CHECKSUM"} )   // a voter is only allowed to vote once per poll with his checksum!
})
public class BallotModel {
	//BallotModel deliberately does not extend BaseModel!
	//No @CreatedDate, @LastModifiedDate or @CreatedBy here.
	//When voting it is a secret who casted this ballot and when.

	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	public Long id;

  /**
	 * Reference to poll
	 * The poll would actually already be coded into the checksum. But we also store the reference for simlicity.
	 */
  @NotNull
  @NonNull
  @ManyToOne
  public PollModel poll;

  /**
	 * level = 0: user voted for himself
	 * level = 1: direct proxy
	 * level = 2: transitive proxy voted
	 * etc. */
  @NonNull   // level must be set in RequiredArgsConstructor
	@NotNull
  public Integer level;

	/**
	 * By default a ballot stands for one vote.
	 * But if other voters delegated their vote to a proxy, then this proxy may vote for that many times.
	 * The voteCount is the sum of all delegated votes plus the vote of the proxy himself.
	 *
	 * IMPORTANT: There are separate ballots for all these delegated votes. The voteCount is just for info. Do not sum it up.
	 */
	public int voteCount = 1;

  /**
   * One vote puts some proposals of this poll into his personally preferred order.
   * One voter may putArray some or all proposals of the poll into his (ordered) ballot. But of course he may only vote at maximum once for every proposal.
   * And one proposal may be voted for by several voters => ManyToMany relationship
   */
  //BE CAREFULL: Lists are not easy to handle in hibernate: https://vladmihalcea.com/hibernate-facts-favoring-sets-vs-bags/
  @NonNull
  @NotNull
  @ManyToMany(fetch = FetchType.EAGER)   //(cascade = CascadeType.MERGE, orphanRemoval = false)
  @OrderColumn  // keep order in DB
  public List<LawModel> voteOrder;   		//proposals in voteOrder must not be duplicate! This is checked in VoteRestController.

  /**
   * Encrypted and anonymous information about the voter that casted this vote into the ballot.
	 * Only the voter knows the voterToken that this checksumModel was created from as
	 *   checksum = hash(voterToken)
	 * If a vote was casted by a proxy, this is still the voters (delegated) checksum.
   */
  @NotNull
  @NonNull
	@OneToOne
	@JoinColumn(name = "CHECKSUM")		// The @Id of a ChecksumModel is the checksum String itself
  public ChecksumModel checksum;

	@Override
	public String toString() {
		String proposalIds = voteOrder.stream().map(law->law.getId().toString()).collect(Collectors.joining(","));
		return "BallotModel{" +
				"id=" + id +
				", poll.id=" + poll.getId() +
				", level=" + level +
				", voteOrder(proposalIds)=[" + proposalIds +"]"+
				", checksum="+checksum +
				"}";
	}
}
