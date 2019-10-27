package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Suggest for improvement for a proposal
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
//@RequiredArgsConstructor
//@ToString(of="id, comment, parent, upVotes, downVotes, createdBy")
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "comments")
public class CommentModel extends BaseModel {
  @NonNull
	@NotNull
	@ManyToOne
	public LawModel proposal;

	/** Suggestion for improvement or comment.  The comment can be written in HTML. */
  @NotNull
  @NonNull
  public String comment;

  /** Author of this comment */
	@CreatedBy
	@NonNull
	@NotNull
	@ManyToOne
	public UserModel createdBy;

	// Bidirectional hierarchical recursive reference - nice1

	/** Parent comment that we reply to */
	@ManyToOne
	@JoinColumn(name = "parentId")
	@JsonBackReference
	public CommentModel parent;

	/** list of replies to this comment */
	// CascadeType.REMOVE => Also Delete child comments (aka "replies" when parent is deleted) https://stackoverflow.com/questions/25967935/jpa-hibernate-spring-onetomany-delete-cascade
	// FYI: default CascadeType https://vladmihalcea.com/a-beginners-guide-to-jpa-and-hibernate-cascade-types/
  @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
  public List<CommentModel> replies = new ArrayList<>();

  /** Users that like this comment. I need the full list of users, nut just the number of upVotes, because we need to prevent duplicate voting. */
	@ManyToMany(fetch = FetchType.EAGER)
	Set<UserModel> upVoters = new HashSet<>();

  /** Users that dislike this comment */
	@ManyToMany(fetch = FetchType.EAGER)
	Set<UserModel> downVoters = new HashSet<>();

  /** get the number of upvotes */
  public int getUpVotes() {
    return upVoters.size();
  }

  /** get the number of downVotes */
  public int getDownVotes() {
    return downVoters.size();
  }


  public CommentModel(LawModel proposal, String comment, CommentModel parent) {
  	this.proposal = proposal;
  	this.comment = comment;
  	this.parent = parent;
	}


	/** @return nicely formatted and useful information for debugging */
	public String toString() {
  	StringBuilder b = new StringBuilder();
  	b.append(this.getClass().getSimpleName());
  	b.append("{id=");
  	b.append(this.getId());
  	b.append(", proposal.id=");
  	b.append(this.getProposal().getId());
  	b.append(", comment='");
  	b.append(this.getComment());
  	b.append(", createdByID=");
  	b.append(this.getCreatedBy().getId());
		b.append(", upVotes=");
		b.append(this.getUpVoters() != null ? this.getUpVoters().size() : "0");
		b.append(", downVotes=");
		b.append(this.getDownVoters() != null ? this.getDownVoters().size() :"0");
		b.append(", numReplies=");
		b.append(this.getReplies() != null ? this.getReplies().size() : "0");
  	b.append(", parentId=");
  	b.append(this.getParent() != null ? this.getParent().getId() : "<null>");
  	b.append('}');
  	return b.toString();
	}
}
