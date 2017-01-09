package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <h2>Model for one Idea</h2>
 *
 * Fields:
 *   - title, mandatory, unique
 *   - description, mandatory
 *   - area, reference to area that his idea is created in
 *   - createdBy, reference to UserModel that created this idea
 *   - createdAt and updatedAt, handled automatically
 *
 * The referenced user and area details are {@link org.doogie.liquido.datarepos.IdeaProjection automatically populated} in the exposed HATEOAS endpoint.
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)   //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ideas")
public class IdeaModel {
  @Id
  @GeneratedValue
  public Long id;

  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String title;

  @NotNull
  @NonNull
  @NotEmpty
  public String description;

  @NonNull
  @NotNull
  @ManyToOne
  AreaModel area;

  /**
   * list of users that support this idea
   */
  @ManyToMany    //(fetch = FetchType.EAGER)    //BUGFIX: EAGER loading is neseccary for testCreateIdeaWithAllRefs to work!
  Set<UserModel> supporters = new HashSet<>();  // I need the full list of names so that no one is allowed to vote twice.


  //TODO: configure createBy
  // http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  // https://blog.countableset.com/2014/03/08/auditing-spring-data-jpa-java-config/    nice example
  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne    //is that necessary? Seems to work without (fetch = FetchType.EAGER)
  public UserModel createdBy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public void addSupporter(UserModel supporter) {
    this.supporters.add(supporter);
  }

  /** Only the number of supportes is exposed via REST */
  public int getNumSupporters() {
    return this.supporters.size();
  }

  public boolean getSupportedByCurrentUser() {
    //if (this.createdBy.equals(getCurrentUser())) return true;
    return false;
  }
}
