package org.doogie.liquido.model;

import lombok.*;
import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.rest.LiquidoRestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.CreatedBy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Optional;

// Using all three lombok annotations Getter,Setter and EqualsAndHashCode does not exactly(!) seem to be the same as the all-in-one @Data annotation
@Getter
@Setter
@EqualsAndHashCode(of = {"title"}, callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "areas")
public class AreaModel extends BaseModel {
  // public fields are automatically exposed in REST endpoint
  @NotNull
  @NonNull
  @Column(unique = true)
  public String title;

  @NotNull
  @NonNull
  public String description;

  @CreatedBy
  @ManyToOne(fetch = FetchType.EAGER)
  //@JoinColumn
  @NonNull
  @NotNull
  public UserModel createdBy;



  public AreaModel(String uri) {
		this("title", "descr", null);
	}

  /* This way one could inline createdBy user information as "creator" field.  But this would unconditionally ALWAYS inline that informatin.  e.g. also  as law.area.creator  in list of laws which I do not want
  public UserModel getCreator() {
    return this.createdBy;
  }
   */

  /** a short info */
	@Override
	public String toString() {
		return "AreaModel[" +
				"title='" + title + '\'' +
				", id=" + id +
				']';
	}
}
