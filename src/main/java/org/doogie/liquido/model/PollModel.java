package org.doogie.liquido.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a poll. A poll has a list of competing proposals
 * A poll starts n days after the initial proposal reached its quorum.
 * And a poll then runs for a configurable number of days.
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
   * Adds an alternative proposal to this post and sets up the two way relationship between them.
   * Alternative proposals can only be added while a poll is in its ELABORATION phase and voting has not yet started.
   * @param alternativeProposal the alternative proposal to add. This poll will be set inside the alternativeProposal
   * @throws Exception when poll is not in its ELABORATION pase any more and voting has already started or is even already finished.
   */
  public void addProposal(LawModel alternativeProposal) throws Exception {
    if (this.getStatus() != PollStatus.ELABORATION) throw new Exception("Cannot add proposal, because poll id="+id+" is not in ELABORATION phase");
    this.proposals.add(alternativeProposal);
    alternativeProposal.setPoll(this);
  }

}
