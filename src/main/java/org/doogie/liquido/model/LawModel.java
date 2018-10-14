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
 * Data model: proposal for ideas, proposals and law.
 */
//Lombok @Data does not work very well with spring. Need to use the individual annotations
@Getter
@Setter
@EqualsAndHashCode(of = {"title"}, callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "laws")
public class LawModel extends BaseModel implements Comparable<LawModel> {
  /*
   * DEPRECATED
   * When a class uses a self references (as we formerly had it in initialLawId, then you must use a sequence for generating IDs,
   * because of the self reference via field "initialLawId".
   * https://vladmihalcea.com/2014/07/15/from-jpa-to-hibernates-legacy-and-enhanced-identifier-generators/
   * https://docs.jboss.org/hibernate/orm/5.0/mappingGuide/en-US/html_single/#identifiers-generators-sequence

  @Id
  @GeneratedValue(strategy=GenerationType.SEQUENCE)
  @SequenceGenerator(name = "sequence", allocationSize = 10)
  public Long id;
  */

  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String title;

  //TODO: lawModel.tagline
  //TODO: lawModel.tags
  //TODO: related ideas? => relations built automatically, when a proposal is added to a running poll.

  @NotNull
  @NonNull
  @NotEmpty
  @Column(length = 1000)
  public String description;

  @NotNull
  @NonNull
  @ManyToOne(optional = false)
  public AreaModel area;

  @ManyToMany(fetch = FetchType.EAGER)
  Set<UserModel> supporters = new HashSet<>();

  /**
   * When in status ELABORATION this is the link to the poll.
   * All alternative proposals point to the same poll.
   * Can be NULL, when this is still an idea or proposal!
   * This is the many side of a bidirectional ManyToOne aggregation relationship.
   * https://vladmihalcea.com/2017/03/29/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/
   *
   */
  @ManyToOne(optional = true)
  //@JoinColumn(name="poll_id")  this column name is already the default
  public PollModel poll = null;

  /** suggestions for improvement */
	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)  // fetch all comments when loading a an idea or proposal.  Prevents "LazyInitializationException, could not initialize proxy - no Session" but at the cost of performance.
	//@Cascade(org.hibernate.annotations.CascadeType.ALL)   // https://vladmihalcea.com/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
  public Set<CommentModel> comments;

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
   * Will be set, when enough likes are added.
   */
  Date reachedQuorumAt;

  /**
   * compare two LawModels by their ID. This is used for sorting proposals in PollModel
   * @param law another idea, proposal or law
   * @return -1, 0 or +1
   */
  @Override
  public int compareTo(LawModel law) {
    if (law == null) return 0;
    return law.getId().compareTo(this.getId());
  }

  /** enumeration of law status */
  public enum LawStatus {
    IDEA(0),            // An idea is a newly created proposal for a law that did not reach its quorum yet.
    PROPOSAL(1),        // When an idea reaches its quorum, then it becomes a proposal and can join a poll.
    ELABORATION(2),     // Proposal is part of a poll and can be discussed. Voting has not yet started.
    VOTING(3),          // When the voting phase starts, the description of a proposals cannot be changed anymore.
    LAW(4),             // The winning proposal becomes a law.
    DROPPED(5),         // All other porposals in the poll are dropped.
    RETENTION(6),       // When a law looses support, it is in the retention phase
    RETRACTED(7);       // When a law looses support for too long, it will be retracted.
    int statusId;
    LawStatus(int id) { this.statusId = id; }
  }

  /** current status of this law */
  @NotNull
  @NonNull
  public LawStatus status =  LawStatus.IDEA;

  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne
  public UserModel createdBy;

  //Remember: You MUST NOT call idea.getSupporters.add(someUser) directly! Because this circumvents the restrictions
  // that there are for supporting an idea. E.g. a user must not support his own idea. Call LawService.addSupporter() instead!

  public int getNumSupporters() {
    if (this.supporters == null) return 0;
    return this.supporters.size();
  }

  public void setDescription(String description) {
    if (this.getStatus() == null ||
        LawStatus.IDEA.equals(this.getStatus()) ||
        LawStatus.PROPOSAL.equals(this.getStatus())) {
      this.description = description;
    } else {
      throw new RuntimeException("Must not change description in status "+this.getStatus());
    }
  }

  /** need some tweaking for a nice and short representation as a string */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("LawModel{");
    buf.append("id=" + id);
    buf.append(", title='" + title + "'");
    buf.append(", area='" + (area != null ? area.getTitle() : "") + "'");
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
