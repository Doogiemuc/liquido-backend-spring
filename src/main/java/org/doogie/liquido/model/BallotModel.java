package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * POJO Entity that represents a vote that a user has casted
 * Fields:
 *  - initialLaw, reference to the original proposal for a law
 *  - voteOrder, ordered list of references to proposals for a law that this user has chosen to order like this.
 *    May include some or all of the alternative proposals (including or not including the initial proposal)
 *  - voterHash, encrypted reference to voter user
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)   //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ballots")
public class BallotModel extends BaseModel {
  //TODO: combined unique key on initialProposal and voterHash
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id;

  @NonNull
  @ManyToOne //(cascade = CascadeType.MERGE)
  public LawModel initialProposal;

  @NonNull
  @NotNull
  @ManyToMany //(cascade = CascadeType.MERGE, orphanRemoval = false)
  //@Column(name = "lawIdOrder")
  public List<LawModel> voteOrder;

  /** encrypted and anonymized information about voter that casted this ballot */
  @NotNull
  @NonNull
  @NotEmpty
  public String voterHash;

  //There is deliberately no @CreatedBy field here! When voting it is a secret who casted this ballot!!!
}
