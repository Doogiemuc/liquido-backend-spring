package org.doogie.liquido.model;

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

@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
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
  //@JoinColumn(name="initialLaw", nullable = false)
  LawModel initialLaw;

  public enum STATUS {
    NEW_PROPOSAL(0),
    ELABORATION(1),
    VOTING(2),
    LAW(3),
    RETENTION(4);
    int statusId;
    STATUS(int id) { this.statusId = id; }
  }

  /** current status of this law, ie. new_proposal, elaboration_phase, voting, law or retention_phase */
  public int status = 0;

  //TODO: configure createBy User for laws: http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)  //TODO: optional = false)
  public UserModel createdBy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  @Override
  public String toString() {
    return "LawModel{" +
      "id=" + id +
      ", title='" + title + '\'' +
      ", description='" + description + '\'' +
      ", initialLaw='" + initialLaw.getTitle() + '\'' +   //BUGFIX: prevent endless loop when initialLaw points to self :-)
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
