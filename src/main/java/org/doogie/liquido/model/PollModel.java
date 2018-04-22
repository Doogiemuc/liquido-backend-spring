package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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
//@RequiredArgsConstructor(suppressConstructorProperties = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "polls")
public class PollModel extends BaseModel {

  /**
   * The proposals for a law of this Poll. All of these proposal must already have reached their quorum.
   * When the poll is in PollStatus == ELABORATION, then these proposals may still be changed and further
   * proposals may be added. When The PollStauts == VOTING, then proposals must not be changed anymore.
   */
  //Beginners guide to Hibernate Cascade types:  https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
  // https://vladmihalcea.com/2017/03/29/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
  @OneToMany(cascade = CascadeType.ALL, mappedBy="poll", fetch = FetchType.EAGER) //, orphanRemoval = true/false ??  Should a proposals be removed when the poll is deleted?
  @NotNull
  @NonNull
  Set<LawModel> proposals = new HashSet<>();         //  ordered by date createdAt, so first in list is the initial proposal



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
  LocalDate votingStartAt;

  /** Date when the voting phase will end. Also set in PollService */
  LocalDate votingEndAt;

  public Long getId() {
    return this.id;
  }

  public int getNumCompetingProposals() {
    if (proposals == null) return 0;
    return proposals.size();
  }

  /**
   * Adds an alternative proposal to this poll and sets up the two way relationship between them.
   * Alternative proposals can only be added while a poll is in its ELABORATION phase and voting has not yet started.
   *
   * @param proposal the proposal to add. This poll will also be linked from proposal. MUST NOT BE NULL.
   * @throws LiquidoException When passed object is not in state PROPOSAL or when poll is not in its ELABORATION phase or not in correct area.
   */
  public void addProposal(@NotNull LawModel proposal) throws LiquidoException {
    //implementation note: I check all business rules down here. As a consequence every caller has to handle the LiquidoException.
    //  Alternative would be to check all this in PollService.java ?
    if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot add proposal(id="+proposal.getId()+") to poll(id="+id+", because proposal is not in state PROPOSAL.");
    if (this.getStatus() != PollStatus.ELABORATION)
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Cannot add proposal, because poll id="+id+" is not in ELABORATION phase");
    if (this.proposals.size() > 0 && !proposal.getArea().equals(this.proposals.iterator().next().getArea()))
      throw new LiquidoException(LiquidoException.Errors.CANNOT_ADD_PROPOSAL, "Added proposal must be in the same area as the other proposals in this poll.");
    this.proposals.add(proposal);
    proposal.setPoll(this);
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
