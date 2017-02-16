package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

/**
 * Data model: proposal for a law.
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "laws")
public class LawModel extends BaseModel {
  /**
   * We must use a sequence for generating Law IDs,
   * because of the self reference via field "initialLawId".
   * https://vladmihalcea.com/2014/07/15/from-jpa-to-hibernates-legacy-and-enhanced-identifier-generators/
   * https://docs.jboss.org/hibernate/orm/5.0/mappingGuide/en-US/html_single/#identifiers-generators-sequence
   */
  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE)
  //@SequenceGenerator(name = "sequence", allocationSize = 10)
  public Long id;

  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String title;

  //TODO: lawModel.tagline
  //TODO: lawModel.tags

  @NotNull
  @NonNull
  @NotEmpty
  public String description;

  @NotNull
  @NonNull
  @ManyToOne(optional = false)
  public AreaModel area;

  /**
   * Reference to initial proposal. The initial proposal references itself.
   * This field must not be null and must point to a valid law ID in the DB.
   *
   * This is a reference to the same Entity. Not easy with hibernate!!! Here are some refs:
   * http://stackoverflow.com/questions/20278222/hibernate-self-reference-entity-as-non-null-column
   * https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
   * http://stackoverflow.com/questions/27414922/unable-to-post-new-entity-with-relationship-using-resttemplate-and-spring-data-r?rq=1
   *
   */
  @ManyToOne(optional = false)
  @NotNull
  @NonNull
  //@JoinColumn(name="initialLawId", referencedColumnName="id", nullable = false)
  @JsonBackReference  // necessary to prevent endless cycle when (de)serializing to/from JSON: http://stackoverflow.com/questions/20218568/direct-self-reference-leading-to-cycle-exception
  LawModel initialLaw;

  public enum LawStatus {
    NEW_ALTERNATIVE_PROPOSAL(0),    // Newly created (alternative) proposal that did not reach its quorum yet
    ELABORATION(1),     // When an idea or an alternative proposal reaches its quorum it is "moved onto the table" and can be discussed and elaborated.
    VOTING(2),          // When the voting phase starts, all proposals can be voted upon
    LAW(3),             // The winning proposal becomes a law.
    RETENTION(4),       // When a law looses support, it is in the retention phase
    RETRACTED(5);       // When a law looses support for too long, it will be retracted.
    int statusId;
    LawStatus(int id) { this.statusId = id; }
  }

  /** current status of this law */
  @NotNull
  @NonNull
  public LawStatus status;

  /*
  @CreatedDate
  @NotNull
  public Date createdAt = new Date();

  @LastModifiedDate
  @NotNull
  public Date updatedAt = new Date();
  */

  //TODO: configure createBy User for laws: http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne
  public UserModel createdBy;

  /** builder for an initial law who's field "initialLaw" points to itself. */
  public static LawModel buildInitialLaw(String title, String description, AreaModel area, LawStatus status, UserModel createdBy) {
    LawModel newLaw = new LawModel();
    newLaw.title = title;
    newLaw.description = description;
    newLaw.area = area;
    newLaw.status = status;
    newLaw.initialLaw = newLaw;   // ref to self
    newLaw.createdBy = createdBy;
    newLaw.createdAt = new Date();
    newLaw.updatedAt = new Date();
    return newLaw;
  }

  @Override
  public String toString() {
    return "LawModel{" +
      "id=" + id +
      ", title='" + title + '\'' +
      ", description='" + description + '\'' +
      ", initialLaw='" + (initialLaw != null ? initialLaw.getTitle() : "<null>") + '\'' +  //BUGFIX: prevent endless loop when initialLaw points to self :-)
      ", status=" + status +
      //", createdBy=" + createdBy +
      ", updatedAt=" + updatedAt +
      ", createdAt=" + createdAt +
      '}';
  }

  /**
   * Two laws are equal, when their id and title are equal.
   * Be careful: Correct equals() and hashCode() methods are crucial for the internals of Hibernate!
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LawModel that = (LawModel) o;

    if (title != that.title) return false;
    if (id != that.id) return false;

    return title != null ? title.equals(that.title) : that.title == null;
  }

  /**
   * Hash code calculate only from ID and title.
   * Keep to contract for evey hashCode() implementation in mind:
   * Two equal objects must have the same hashcode. The inverse is not necessarily true: Two objects with
   * the same hashCode must not necessarily be equal.
   * @return hash code value
   */
  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (title != null ? title.hashCode() : 0);
    return result;
  }
}
