package org.doogie.liquido.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.doogie.liquido.model.converter.MatrixConverter;
import org.doogie.liquido.rest.deserializer.LawModelDeserializer;
import org.doogie.liquido.util.Matrix;
import org.hibernate.validator.constraints.UniqueElements;
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
@Entity
@Data
@NoArgsConstructor  		// BUGFIX: lombok data includes @RequiredArgsConstructor, but does not include @NoArgsConstructor !
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "polls")
public class PollModel extends BaseModel {

	/** The title of a poll must be unique. It can be edited by anyone who has a proposal in this poll. */
	@Column(unique=true)    //TODO: @UniqueConstraint:  title in team is unique
	@NonNull
	@NotNull
	String title;

	@NonNull
	@NotNull
	@OneToOne
	AreaModel area;

	@ManyToOne(fetch = FetchType.LAZY)
	TeamModel team;

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
	   When creating a new poll via POST /polls/add  , then the first proposal can be passed as URI:   {"title":"Poll created by test 1582701066468","proposals":["http://localhost:8080/liquido/v2/laws/405"]}
	   To make that work, the content of the HashSet, ie. the URI will be deserialized with LawModelDeserializer.class
	*/
  @OneToMany(cascade = CascadeType.MERGE, mappedBy="poll", fetch = FetchType.EAGER) //, orphanRemoval = true/false ??  Should a proposals be removed when the poll is deleted? => NO
  //@JsonDeserialize(contentUsing = LawModelDeserializer.class)  //  If I do this then deserialization of LawModels does not work anymore ?????? Why ?????
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
  LocalDateTime votingStartAt = null;

  /** Date when the voting phase will end. Will be set in PollService */
	LocalDateTime votingEndAt = null;

	/** The wining proposal of this poll, that became a proposal. Filled after poll is FINISHED. */
	@OneToOne
	LawModel winner = null;

	/**
	 * The calculated duelMatrix when the voting phase is finished.
	 * This is set in {@link org.doogie.liquido.services.PollService#finishVotingPhase(PollModel)}
	 * This attribute is serialized as JSON array of arrays and then stored as VARCHAR
	 */
	@Convert(converter = MatrixConverter.class)
	Matrix duelMatrix = null;

	//Implementation note: A poll does not contain a link to its BallotModels. We do not want to expose the ballots while the voting phase is still running.

  /** return the number of competing proposals */
  public int getNumCompetingProposals() {
    if (proposals == null) return 0;
    return proposals.size();
  }

  @Override
  public String toString() {
  	StringBuilder sb = new StringBuilder()
			.append("PollModel[")
			.append("id=").append(id)
			.append(", status=").append(status)
			.append(", title='").append(title).append("'") 
			.append(", area.id=");
  	if (proposals != null) {		// be carefull and clean in toString()   :-)
    	if (proposals.size() > 0) sb.append(", area.id=").append(getArea().id);
    	sb.append(", numProposals="+this.proposals.size());
		}
  	sb.append(']');
  	return sb.toString();
  }
}
