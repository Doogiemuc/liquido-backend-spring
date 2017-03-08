package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Data model: proposal for a law.
 */
@Data
@EqualsAndHashCode(of = {"id", "title"}, callSuper = false)
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
  @Column(length = 1000)
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
    NEW_PROPOSAL(0),    // Newly created proposal that did not reach its quorum yet. This is an initialLaw, when this.initialLaw == this
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

  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne
  public UserModel createdBy;

  /** builder for an initial law who's field "initialLaw" points to itself. */
  public static LawModel buildInitialLaw(String title, String description, AreaModel area, UserModel createdBy) {
    LawModel newLaw = new LawModel();
    newLaw.title = title;
    newLaw.description = description;
    newLaw.area = area;
    newLaw.status = LawStatus.NEW_PROPOSAL;
    newLaw.initialLaw = newLaw;   // ref to self
    newLaw.createdBy = createdBy;
    newLaw.createdAt = new Date();
    newLaw.updatedAt = new Date();
    return newLaw;
  }

  /** need some tweaking for a nice and short representation as a string */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();

    buf.append("LawModel{");
    buf.append("id=" + id);
    buf.append(", title='" + title + '\'');
    buf.append(", description='");
    if (description != null && description.length() > 100) {
      buf.append(description.substring(0, 100));
      buf.append("...");
    } else {
      buf.append(description);
    }
    buf.append('\'');
    buf.append(", initialLaw.title='" + (initialLaw != null ? initialLaw.getTitle() : "<null>") + '\'');  //BUGFIX: prevent endless loop when initialLaw points to self :-)
    buf.append(", status=" + status);
    buf.append(", createdBy.email=" + (createdBy != null ? createdBy.getEmail() : "<null>"));
    buf.append(", updatedAt=" + updatedAt);
    buf.append(", createdAt=" + createdAt);
    buf.append('}');
    return buf.toString();
  }

}
