package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

  @ManyToMany
  Set<UserModel> supporters = new HashSet<>();

  //TODO:  make poll optional and remove IdeaModel completely?  Create poll when NEW_PROPOSAL reaches its quorum...
  /**
   * A poll is automatically created, when the initial law reaches its quorum.
   * All alternative proposals point to the same poll.
   */
  @NotNull
  @NonNull
  @ManyToOne(optional = false)
  public PollModel poll;

  /*   DEPRECATED
   * Reference to initial proposal. The initial proposal references itself.
   * This field must not be null and must point to a valid law ID in the DB.
   *
   * This is a reference to the same Entity. Not easy with hibernate!!! Here are some refs:
   * http://stackoverflow.com/questions/20278222/hibernate-self-reference-entity-as-non-null-column
   * https://vladmihalcea.com/2015/03/05/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
   * http://stackoverflow.com/questions/27414922/unable-to-post-new-entity-with-relationship-using-resttemplate-and-spring-data-r?rq=1

  @ManyToOne(optional = false)
  @NotNull
  @NonNull
  //@JoinColumn(name="initialLawId", referencedColumnName="id", nullable = false)
  @JsonBackReference  // necessary to prevent endless cycle when (de)serializing to/from JSON: http://stackoverflow.com/questions/20218568/direct-self-reference-leading-to-cycle-exception
  LawModel initialLaw;
  */

  /**
   * Date when this proposal reached its quorum.
   * Will be set, when new likes are added.
   * n days after the initialProposal reaches its quorum, then the voting phase starts.
   */
  Date reachedQuorumAt;


  public enum LawStatus {
    NEW_PROPOSAL(0),    // Newly created proposal for a law that did not reach its quorum yet.
                        // This is an initialLaw, when this.initialLaw == this
    ELABORATION(1),     // When a proposal reaches its quorum, then a poll is created and further alternative proposals can be added.
    VOTING(2),          // When the voting phase starts, these proposals can be voted upon.
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

  /**
   * @return true when this is the initial proposal of the poll
   */
  public boolean isInitialProposal() {
    return (getPoll() != null) && this.equals(getPoll().getInitialProposal());
  }

  /**
   * Call this when a user 'likes' a proposal
   * No user will be added twice and the creator of this proposal cannot be added as supporter.
   * @param supporter The user that wants to discuss this idea. Must not be the creator!
   * @throws RuntimeException When you try to add the creator as supporter.
   */
  public void addSupporter(UserModel supporter) {
    if (supporter == null) return;
    if (supporter.equals(this.getCreatedBy())) {
      throw new RuntimeException("a proposal for a law cannot be supported by its creator");
    }
    this.supporters.add(supporter);
  }

  public void addSupporters(Set<UserModel> supporters) {
    for(UserModel supporter : supporters) {
      this.addSupporter(supporter);
    }
  }


  public int getNumSupporters() {
    return this.supporters.size();
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
    buf.append(", poll.id=" + (poll != null ? poll.getId() : "<null>"));
    buf.append(", status=" + status);
    buf.append(", numSupporters=" + getNumSupporters());
    buf.append(", createdBy.email=" + (createdBy != null ? createdBy.getEmail() : "<null>"));
    buf.append(", reachedQuorumAt=" + reachedQuorumAt);
    buf.append(", updatedAt=" + updatedAt);
    buf.append(", createdAt=" + createdAt);
    buf.append('}');
    return buf.toString();
  }

}
