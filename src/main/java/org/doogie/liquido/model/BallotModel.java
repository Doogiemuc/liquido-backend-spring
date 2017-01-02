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

/** POJO Entity that represents a vote that a user has casted */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ballots")
public class BallotModel {
  //protected fields are only possible with Google's GSON lib (not with Jackson)
  @Id
  @GeneratedValue
  private Long id;

  @NonNull
  @NotNull
  @OneToOne
  public LawModel initialLaw;

  @NonNull
  @NotNull
  @OneToMany
  public List<LawModel> voteOrder;

  /** encrypted information about voter that casted this ballot */
  @NotNull
  @NonNull
  @NotEmpty
  public String voterHash;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

}
