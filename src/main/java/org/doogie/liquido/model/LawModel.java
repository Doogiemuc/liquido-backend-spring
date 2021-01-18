package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;
import org.doogie.liquido.rest.converters.PollAsLinkJsonSerializer;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * <h3>Data model for an idea, proposal or law</h3>
 *
 * This is the central model class in Liquido.
 *
 * Any user can add an ideas. Once an idea reaches its quorum, then it becomes a proposal. When a proposal joins
 * a poll, then it can be discussed and elaborated. When the voting phase of the poll starts, then a
 * proposal must not be changed anymore. Users can vote in the poll. When the voting phase is finished,
 * then the winning proposal becomes a law. All other proposals in the poll are dropped.
 *
 * The title of every idea must be globally unique!
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"title"}, callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "laws")
public class LawModel extends BaseModel implements Comparable<LawModel> {

  @NotNull
  @NonNull
  @Column(unique = true)
  public String title;

	/**
	 * HTML description of this proposal. This description can only be edited by the creator
	 * as long as the proposal is not yet in a poll in voting phase.
	 */
  @NotNull
  @NonNull
  @Column(length = 1000)
  public String description;

  /** Area of this idea/proposal/proposal */
  @NotNull
  @NonNull
  @ManyToOne(optional = false)
  public AreaModel area;

	/** enumeration for proposal status */
	public enum LawStatus {
		IDEA(0),            // An idea is a newly created proposal for a proposal that did not reach its quorum yet.
		PROPOSAL(1),        // When an idea reaches its quorum, then it becomes a proposal and can join a poll.
		ELABORATION(2),     // Proposal is part of a poll and can be discussed. Voting has not yet started.
		VOTING(3),          // When the voting phase starts, the description of a proposals cannot be changed anymore.
		LAW(4),             // The winning proposal becomes a proposal.
		LOST(5),         		// All non winning proposals in a finished poll are dropped. They lost the vote.
		RETENTION(6),       // When a proposal looses support, it is in the retention phase
		RETRACTED(7);       // When a proposal looses support for too long, it will be retracted.
		int statusId;
		LawStatus(int id) { this.statusId = id; }
	}

	/** current status of this proposal */
	@NotNull
	@NonNull
	public LawStatus status =  LawStatus.IDEA;

  /**
	 * Users that support this.
	 * This is not just a counter, because there are restrictions:
	 *  - A user must not support an idea, proposal or law more than once.
	 *  - A user must not support his own idea, proposal or law.
	 *  - When a new support is added, this idea might become a proposal.
	 *
	 * This attribute is private, so that you cannot (and must not) call idea.getSupporters.add(someUser)
	 * Instead you must use LawService.addSupporter()   or POST to /laws/{id}/like
	 */
	@JsonIgnore  // do not serialize when returning JSON. Only return this.getNumSupporters()
	@RestResource(exported = false)		// supportes are not exposed as Spring Data REST resource directly.
  @ManyToMany(fetch = FetchType.EAGER)
  private Set<UserModel> supporters = new HashSet<>();

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
  @JsonBackReference  // necessary to prevent endless cycle when (de)serializing to/from JSON: http://stackoverflow.com/questions/20218568/direct-self-reference-leading-to-cycle-exception
	//TODO: serialize LawModel.poll as HATEOAS link  => this has consequences for the client that in some places needs the information of the proposal's poll, eg. in LawPanel!  Solution: Add poll.numCompeting proposals as @Transient attribute
	//@JsonProperty("_links")   															// JSON will contain "_links.poll.href"
	//@JsonSerialize(using = PollAsLinkJsonSerializer.class)  // We only return an HATEOS link to the poll
  public PollModel poll = null;

  /** Comments and suggestions for improvement for this proposal*/
	@JsonIgnore  																											// To fetch comments via REST user CommentProjection of CommentModel
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)  	//TODO If you get a "LazyInitializationException, could not initialize proxy - no Session" you might need to pre load comments of a proposal. We should not generally change this to FetchTypoe.EAGER for performance reasons. Check with tests if FetchType.LAZY is enough
	//@Cascade(org.hibernate.annotations.CascadeType.ALL)   					// https://vladmihalcea.com/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
  public Set<CommentModel> comments = new HashSet<>();							// Comments are deliberately a Set and not a List. There are no duplicates.

  /**
   * Date when this proposal reached its quorum.
   * Will be set, when enough likes are added.
   */
  LocalDateTime reachedQuorumAt;

	/** The user that initially created the idea */
	@CreatedBy  // automatically handled by spring data jpa auditing
	@ManyToOne
	public UserModel createdBy;


	//MAYBE: lawModel.tags or related ideas? => relations built automatically, when a proposal is added to a running poll.




	/**
   * compare two LawModels by their ID. This is used for sorting proposals in PollModel
   * @param law another idea, proposal or proposal
   * @return -1, 0 or +1
   */
  @Override
  public int compareTo(LawModel law) {
    if (law == null) return 0;
    return law.getId().compareTo(this.getId());
  }

  //Remember: You MUST NOT call idea.getSupporters.add(someUser) directly! Because this circumvents functional restrictions.
  //E.g. a user must not support his own idea. Call LawService.addSupporter() instead!
  public int getNumSupporters() {
    if (this.supporters == null) return 0;
    return this.supporters.size();
  }

  private static int getNumChildComments(CommentModel c) {
  	int count = 0;
  	if (c == null || c.getReplies() == null || c.getReplies().size() == 0) return 0;
		for (Iterator<CommentModel> it = c.getReplies().iterator(); it.hasNext(); ) {
			CommentModel reply = it.next();
			count += 1 + getNumChildComments(reply);
		}
		return count;
	}

	/**
	 * Number of comments and (recursive) replies.
	 * @return overall number of comments
	 */
	@JsonIgnore  // prevent LazyInitialisationException for getter - comments might not be loaded
	public int getNumComments() {
  	if (this.comments == null || comments.size() == 0) return 0;
  	int count = 0;
		for (Iterator<CommentModel> it = comments.iterator(); it.hasNext(); ) {
			CommentModel c = it.next();
			count += 1 + getNumChildComments(c);
		}
		//Optional<Integer> countStream = comments.stream().map(LawModel::getNumChildComments).reduce(Integer::sum);
		return count;
	}

	/**
	 * Description can only be changed when in status IDEA or PROPOSAL
	 * @param description the new description (HTML)
	 */
  public void setDescription(String description) {
    if (LawStatus.IDEA.equals(this.getStatus()) ||
        LawStatus.PROPOSAL.equals(this.getStatus()) ||
				LawStatus.ELABORATION.equals(this.getStatus())
		) {
      this.description = description;
    } else {
      throw new RuntimeException("Must not change description in status "+this.getStatus());
    }
  }

  /** Stringify mostly all info about this idea, proposal or law */
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
		buf.append(", numComments=" + getNumComments());					// keep in mind  that comments are loaded lazily. So toString MUST be called within a Hibernate Session
    buf.append(", createdBy.email=" + (createdBy != null ? createdBy.getEmail() : "<null>"));
    buf.append(", reachedQuorumAt=" + reachedQuorumAt);
    buf.append(", updatedAt=" + updatedAt);
    buf.append(", createdAt=" + createdAt);
    buf.append('}');
    return buf.toString();
  }

	/** Nice and short representation of an Idea, Proposal or Law as a string */
  public String toStringShort() {
		StringBuilder buf = new StringBuilder();
		buf.append("LawModel{");
		buf.append("id=" + id);
		buf.append(", title='" + title + "'");
		if (poll != null) buf.append(", poll.id=" + poll.getId());
		buf.append(", status=" + status);
		buf.append(", createdBy.email=" + (createdBy != null ? createdBy.getEmail() : "<null>"));
		buf.append('}');
		return buf.toString();
	}

}
