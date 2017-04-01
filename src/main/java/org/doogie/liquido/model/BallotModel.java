package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * POJO Entity that represents a vote that a user has casted
 * Fields:
 *  - initialLaw: reference to the original proposal for a law
 *  - voteOrder: ordered list of references to proposals for a law that this user has chosen to order like this.
 *    May include some or all of the alternative proposals (including or not including the initial proposal).
 *    Must not be null.
 *  - voterHash: anonymized reference to the voter
 */
@Data
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)   //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ballots", uniqueConstraints= {
  @UniqueConstraint(columnNames = {"POLL_ID", "voterToken"})   // a voter is only allowed to vote once per poll!
})
public class BallotModel extends BaseModel {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long id;

  /** reference to poll */
  @NotNull
  @NonNull
  @ManyToOne
  public PollModel poll;

  /** preferred order of proposals of this specific voter */
  @NonNull
  @NotNull
  @ManyToMany   //(cascade = CascadeType.MERGE, orphanRemoval = false)
  @OrderColumn  // keep order in DB
  public List<LawModel> voteOrder;   //TODO: laws in voteOrder must not be duplicate!!!

  /** encrypted and anonymized information about the voter that casted this ballot */
  @NotNull
  @NonNull
  @NotEmpty
  public String voterToken;

  //There is deliberately no @CreatedBy field here! When voting it is a secret who casted this ballot!!!

}
