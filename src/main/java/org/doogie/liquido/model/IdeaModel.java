package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
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
 * The referenced user and area details can be {@link org.doogie.liquido.model.IdeaProjection automatically populated} in the exposed HATEOAS endpoint when querying for the projection:
 * http://localhost:8090/liquido/v2/ideas/1?projection=ideaProjection
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)   //BUGFIX: https://jira.spring.io/browse/DATAREST-884
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ideas")
public class IdeaModel extends BaseModel {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
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
  @ManyToOne(optional = false)
  AreaModel area;

  // created by will automatically be set to the currently logged in user on save
  // http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  // https://blog.countableset.com/2014/03/08/auditing-spring-data-jpa-java-config/    nice example
  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne(optional = false)
  public UserModel createdBy;

  /**
   * List of users that support this idea.
   * I need the full list of references, so that no user can support this idea twice.
   */
  @ManyToMany(fetch = FetchType.EAGER)    //BUGFIX: EAGER loading is necessary for testCreateIdeaWithAllRefs to work!
  Set<UserModel> supporters = new HashSet<>();

  /**
   * Call this when a user wants to discuss this idea further.
   * No user will be added twice!
   * @param supporter The user that wants to discuss this idea.
   */
  public void addSupporter(UserModel supporter) {
    if (supporter == null) return;
    if (supporter.equals(this.getCreatedBy())) {
      throw new RuntimeException("an idea must not be supported by its creator");
    }
    this.supporters.add(supporter);
  }

  /** For convenience we directly expose the number of supporters */
  public int getNumSupporters() {
    return this.supporters.size();
  }

}
