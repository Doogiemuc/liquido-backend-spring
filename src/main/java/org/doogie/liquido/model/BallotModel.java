package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * POJO Entity that represents a vote that a user has casted for one given poll.
 *
 * Each ballot contains the ordered list of proposals that this user voted for.
 * But the ballot does *NOT* contain any reference to the voter.
 * A vote must be anonymous.
 *
 * But in Liquido, the ballot has a ballotToken which is created from the users voterToken.
 *
 *   ballotToken = hash(voterToken)
 *
 * Only the voter knows his own voterToken. So only he can proof that this actually is his ballot
 * by providing the correct input to the hash function.
 *
 * This way a voter can even update his ballot as long as the voting phase is still open.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@NoArgsConstructor
@RequiredArgsConstructor  //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ballots", uniqueConstraints= {
  @UniqueConstraint(columnNames = {"POLL_ID", "areaToken"})   // a voter is only allowed to vote once per poll!
})
public class BallotModel extends BaseModel {
  /** reference to poll */
  @NotNull
  @NonNull
  @ManyToOne
  public PollModel poll;

  /** did the user vote for his own? Then never overwrite this ballot from a proxy */
  @NonNull   // needed, even though Lombok shows a warning
  public boolean ownVote;

  /**
   * One vote puts some proposals of this poll into his personally preferred order.
   * One voter may put some or all proposals of the poll into his (ordered) ballot. But of course he may only vote at maximum once for every proposal.
   * And one proposal may be voted for by several voters => ManyToMany relationship
   */
  @NonNull
  @NotNull
  @ManyToMany   //(cascade = CascadeType.MERGE, orphanRemoval = false)
  @OrderColumn  // keep order in DB
  public List<LawModel> voteOrder;   //laws in voteOrder must not be duplicate! This is checked in VoteRestController.

  /**
   * encrypted and anonymized information about the voter that casted this vote into the ballot.
   * This areaToken ins confidential. It MUST only be stored on the server. Only the user knows
   * the voterToken that hashes to this areaToken.
   */
  @NotNull
  @NonNull
  @NotEmpty
  public String areaToken;    // ballotToken = hash(voterToken)

  //There is deliberately no @CreatedBy field here! When voting it is a secret who casted this ballot!!!


	// DO NOT expose areaToken in toString !!!
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		voteOrder.stream().map(law -> buf.append(law.getId()) );
		return "BallotModel{" +
				"id=" + id +
				", poll.id=" + poll.getId() +
				", ownVote=" + ownVote +
				", voteOrder(proposalIds)=[" + buf.toString() +
				"]}";
	}
}
