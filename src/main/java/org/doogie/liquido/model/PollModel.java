package org.doogie.liquido.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SortNatural;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data model for a poll. A poll has a list of competing proposals
 * A poll CAN be started by the creator of an idea, when this idea reaches its quorum and becomes a proposal.
 * A poll then runs for a configurable number of days.
 *
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

  /**
   * The proposals for a law in this poll. All of these proposal must already have reached their quorum.
	 * There must not be any duplicates. A proposal can join a poll only once.
   * When the poll is in PollStatus == ELABORATION, then these proposals may still be changed and further
   * proposals may be added. When The PollStauts == VOTING, then proposals must not be changed anymore.
   */
  // This is the ONE side of a bidirectional ManyToOne aggregation relationship.
  // https://vladmihalcea.com/2017/03/29/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
  // Beginners guide to Hibernate Cascade types:  https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/

	// we deliberately fetch all proposals in this poll EAGERly, so that getNumCompetingProposals can be called on the returned entity.

	// I had problems with ArrayList: https://stackoverflow.com/questions/1995080/hibernate-criteria-returns-children-multiple-times-with-fetchtype-eager
	// So I used a SortedSet:   https://docs.jboss.org/hibernate/orm/5.2/userguide/html_single/Hibernate_User_Guide.html#collections-sorted-set
  @OneToMany(cascade = CascadeType.MERGE, mappedBy="poll", fetch = FetchType.EAGER) //, orphanRemoval = true/false ??  Should a proposals be removed when the poll is deleted? => NO
  @NotNull
  @NonNull
	@SortNatural		// sort proposals in this poll by their ID  (LawModel implements Comparable)
	SortedSet<LawModel> proposals = new TreeSet<>();

  // Keep in mind that you SHOULD NOT just simply call
  //   anyPoll.getProposals.add(someProposal)
  // Because this circumvents all the restrictions that there are for adding a proposals to a poll!
  // Instead use PollService.addProposalToPoll(proposals, poll) !

  public enum PollStatus {
    ELABORATION(0),     // When the initial proposal reaches its quorum, the poll is created. Alternative proposals can be added in this phase.
    VOTING(1),          // When the voting phase starts, all proposals can be voted upon. No more alternative proposals can be added. Proposals cannot be edited in this phase.
    FINISHED(2);        // The winning proposal becomes a law.
    int statusId;
    PollStatus(int id) { this.statusId = id; }
  }

  /** initially a poll is in its elaboration phase, where further proposals can be added */
  PollStatus status = PollStatus.ELABORATION;

  /** Date when the voting phase started. Will be set in PollService */
  //TODO: LocalDateTime (without timezone)  or ZonedDateTime (with timezone)?
  LocalDateTime votingStartAt;

  /** Date when the voting phase will end. Will be set in PollService */
	LocalDateTime votingEndAt;

	/** The wining proposal of this poll, that became a law. Filled after poll is FINISHED. */
	@OneToOne
	LawModel winner = null;

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
        ", numProposals=" + (proposals != null ? proposals.size() : "<NULL>") +
        ", status=" + status +
        '}';
  }
}
