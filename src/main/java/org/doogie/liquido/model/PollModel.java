package org.doogie.liquido.model;

import lombok.*;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a poll. A poll has a list of competing proposals
 * A poll CAN be started by the creator of an idea, when this idea reaches its quorum and becomes a proposal.
 * A poll then runs for a configurable number of days.
 */
@Data
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Entity
@NoArgsConstructor
//@RequiredArgsConstructor(suppressConstructorProperties = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "polls")
public class PollModel extends BaseModel {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  public Long id;

  //Beginners guide to Hibernate Cascade types:  https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
  @OneToMany(cascade = CascadeType.ALL, mappedBy="poll", fetch = FetchType.EAGER) //, orphanRemoval = true/false ??? ) Should proposals be removed when the poll is deleted?
  @NotNull
  @NonNull
  List<LawModel> proposals = new ArrayList<>(); //  ordered by date createdAt, so first in list is the initial proposal

  public enum PollStatus {
    ELABORATION(0),     // When the initial proposal reaches its quorum, the poll is created. Alternative proposals can be added in this phase.
    VOTING(1),          // When the voting phase starts, all proposals can be voted upon. No more alternative proposals can be added.
    FINISHED(2);        // The winning proposal becomes a law.
    int statusId;
    PollStatus(int id) { this.statusId = id; }
  }

  /** initially a poll is in its elaboration phase, where further proposals can be added */
  PollStatus status = PollStatus.ELABORATION;

  public Long getId() {
    return this.id;
  }

  public LawModel getInitialProposal() {
    return proposals.get(0);
  }

  public int getNumCompetingProposals() {
    if (proposals == null) return 0;
    return proposals.size();
  }

  /**
   * Adds an alternative proposal to this poll and sets up the two way relationship between them.
   * Alternative proposals can only be added while a poll is in its ELABORATION phase and voting has not yet started.
   * @param proposal the proposal to add. This poll will also be linked from proposal. MUST NOT BE NULL.
   * @throws LiquidoException When passed object is not in state PROPOSAL or when poll is not in its ELABORATION phase.
   */
  public void addProposal(@NotNull LawModel proposal) throws LiquidoException {
    if (proposal.getStatus() != LawModel.LawStatus.PROPOSAL) throw new LiquidoException("Cannot add proposal(id="+proposal.getId()+") to poll(id="+id+", because proposal is not in state PROPOSAL.");
    if (this.getStatus() != PollStatus.ELABORATION) throw new LiquidoException("Cannot add proposal, because poll id="+id+" is not in ELABORATION phase");
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
