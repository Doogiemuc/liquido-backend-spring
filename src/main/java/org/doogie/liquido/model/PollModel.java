package org.doogie.liquido.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.doogie.liquido.model.converter.MatrixConverter;
import org.doogie.liquido.util.Matrix;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data model for a poll. A poll has a list of competing proposals
 * A poll CAN be started by the creator of an idea, when this idea reaches its quorum and becomes a proposal.
 * A poll then runs for a configurable number of days.
 * The votes within a poll are stored in anonymous ballots.
 * This is just a domain model class. All the business logic is in {@link org.doogie.liquido.services.PollService}!
 */
@Data
@Entity
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
//@RequiredArgsConstructor(suppressConstructorProperties = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "polls")
public class PollModel extends BaseModel {

	public PollModel(String title) {
		this.title = title;
	}

	/** The title of a poll can be edited by anyone who has a proposal in this poll. */
	@Nullable
	String title;

  /**
   * The set of proposals in this poll. All of these proposal must already have reached their quorum.
	 * There cannot be any duplicates. A proposal can join a poll only once.
   * When the poll is in PollStatus == ELABORATION, then these proposals may still be changed and further
   * proposals may be added. When The PollStatus == VOTING, then proposals must not be added or be changed anymore.
   */
  /* Implementation notes:
     This is the ONE side of a bidirectional ManyToOne aggregation relationship.
     Keep in mind that you must not call  poll.proposals.add(prop). Because this circumvents all the restrictions that there are for adding a proposals to a poll!
     Instead use PollService.addProposalToPoll(proposals, poll) !
	   We deliberately fetch all proposals in this poll EAGERly, so that getNumCompetingProposals can be called on the returned entity.
	*/
  @OneToMany(cascade = CascadeType.MERGE, mappedBy="poll", fetch = FetchType.EAGER) //, orphanRemoval = true/false ??  Should a proposals be removed when the poll is deleted? => NO
  @NotNull
  @NonNull
	Set<LawModel> proposals = new HashSet<>();

  // Some older notes, when proposals still was a SortedSet.  Not relevant anymore, but still very interesting reads!
	// https://vladmihalcea.com/2017/03/29/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
	// Beginners guide to Hibernate Cascade types:  https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
	// I had problems with ArrayList: https://stackoverflow.com/questions/1995080/hibernate-criteria-returns-children-multiple-times-with-fetchtype-eager
	// So I used a SortedSet:   https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#collections-sorted-set   => Therefore LawModel must implement Comparable
	// See also https://vladmihalcea.com/hibernate-facts-favoring-sets-vs-bags/
	// @SortNatural		// sort proposals in this poll by their ID  (LawModel implements Comparable)

  public enum PollStatus {
    ELABORATION(0),     // When the initial proposal reaches its quorum, the poll is created. Alternative proposals can be added in this phase.
    VOTING(1),          // When the voting phase starts, all proposals can be voted upon. No more alternative proposals can be added. Proposals cannot be edited in this phase.
    FINISHED(2);        // The winning proposal becomes a proposal.
    int statusId;
    PollStatus(int id) { this.statusId = id; }
  }

  /** initially a poll is in its elaboration phase, where further proposals can be added */
  PollStatus status = PollStatus.ELABORATION;

  /** Date when the voting phase started. Will be set in PollService */
  @JsonSerialize(converter = )
  LocalDateTime votingStartAt;			// LocalDateTime is serialized as Array: [year, month, day, hour, minute, second, millisecond]

  /** Date when the voting phase will end. Will be set in PollService */
	LocalDateTime votingEndAt;

	/** The wining proposal of this poll, that became a proposal. Filled after poll is FINISHED. */
	@OneToOne
	LawModel winner = null;


	/**
	 * The calculated duelMatrix when the voting phase is finished.
	 * This is set in {@link org.doogie.liquido.services.PollService#finishVotingPhase(PollModel)}
	 * This attribute is serialized as JSON array of arrays and then stored as VARCHAR
	 */
	@Convert(converter = MatrixConverter.class)
	Matrix duelMatrix;

	//Implementation note: A poll does not contain a link to its BallotModels. We do not want to expose the ballots while the voting phase is still running.

  /** return the number of competing proposals */
  public int getNumCompetingProposals() {
    if (proposals == null) return 0;
    return proposals.size();
  }

  public AreaModel getArea() {
  	if (getNumCompetingProposals() == 0) throw new RuntimeException("poll has no area, because there are no proposals in it yet.");   // This should never happen
	  return this.proposals.iterator().next().getArea();
  }

  @Override
  public String toString() {
    return "PollModel{" +
        "id=" + id +
        ", status=" + status +
		    ", title='" + title + "'" +
		    ", numProposals=" + (proposals != null ? proposals.size() : "<NULL>") +
        '}';
  }
}
