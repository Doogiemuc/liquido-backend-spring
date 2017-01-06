package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Entity
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "laws")
public class LawModel {
  /**
   * We must use a sequence for generating Law IDs,
   * because of the self reference via field "initialLawId".
   * https://vladmihalcea.com/2014/07/15/from-jpa-to-hibernates-legacy-and-enhanced-identifier-generators/
   */
  @Id
  @GeneratedValue(generator = "sequence", strategy=GenerationType.SEQUENCE)
  @SequenceGenerator(name = "sequence", allocationSize = 10)
  private Long id;

  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String title;

  @NotNull
  @NonNull
  @NotEmpty
  public String description;

  /**
   * Reference to initial proposal. The initial proposal references itself.
   * This field must not be null and must point to a valid law ID in the DB.
   *
   * This is a reference to the same Entity. Not easy with hibernate!!! Here are some refs:
   * http://stackoverflow.com/questions/20278222/hibernate-self-reference-entity-as-non-null-column
   * https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
   * http://stackoverflow.com/questions/27414922/unable-to-post-new-entity-with-relationship-using-resttemplate-and-spring-data-r?rq=1
   *
   * There is deliberately no Lombok "@NonNull" annotation here on the "initialLaw" field, because then Lombok would require
   * this parameter in the RequiredArgsConstructor and I could never create an instance of the "first ever" law in Java, because of the endless loop.
   */
  @ManyToOne(optional = false)
  @NotNull
  //@JoinColumn(name="initialLawId", referencedColumnName="id", nullable = false)
  @JsonBackReference  // necessary to prevent endless cycle when (de)serializing to/from JSON: http://stackoverflow.com/questions/20218568/direct-self-reference-leading-to-cycle-exception
  LawModel initialLaw;

  public enum LawStatus {
    NEW_PROPOSAL(0),
    ELABORATION(1),
    VOTING(2),
    LAW(3),
    RETENTION(4);
    int statusId;
    LawStatus(int id) { this.statusId = id; }
  }

  /** current status of this law */
  public LawStatus status = LawStatus.NEW_PROPOSAL;

  //TODO: configure createBy User for laws: http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  @CreatedBy
  @NonNull
  @NotNull // can't be set, otherwise hibernate throws "Not-null property references a transient value"  on insert
  @ManyToOne  //TODO: (optional = true, fetch = FetchType.EAGER)
  public UserModel createdBy;


  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;


  public LawModel(String title, String description, LawModel initialLaw, LawStatus status, UserModel createdBy) {
    if (initialLaw == null || createdBy == null) throw new IllegalArgumentException("initialLaw and createdBy must not be null!");
    this.title = title;
    this.description = description;
    this.initialLaw = initialLaw;
    this.status = status;
    this.createdBy = createdBy;
    this.createdAt = new Date();
    this.updatedAt = new Date();
  }

  /** builder for an initial law whos field "initialLaw" points to itself. */
  public static LawModel buildInitialLaw(String title, String description, LawStatus status, UserModel createdBy) {
    LawModel newLaw = new LawModel();
    newLaw.title = title;
    newLaw.description = description;
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
