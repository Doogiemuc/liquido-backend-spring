package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * One user / voter / citizen
 */
@Data
@EqualsAndHashCode(callSuper = true)
//@ToString(of="id, email, profile")   // This is extremely important! Do not exposed password in toString() !!!
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "users")
public class UserModel extends BaseModel {
  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String email;

  @NotNull
  @NonNull
  @NotEmpty
  @JsonIgnore                       // tell jackson to not serialize this field
  //@Getter(AccessLevel.PRIVATE)      // Lombok getter cannot be private because I need access to the password in LiquidoUserDetailsService.java
  @RestResource(exported = false)   // private: never exposed via REST!
  private String passwordHash;

  @Embedded
  public UserProfileModel profile;

  /** all the delegees that this proxy may vote for */
  @ElementCollection(fetch = FetchType.EAGER)  //BUGFIX: needed to prevent LazyInitializationException   http://stackoverflow.com/questions/22821695/lazyinitializationexception-failed-to-lazily-initialize-a-collection-of-roles
  @CollectionTable(name = "USERS_VOTER_TOKENS")
  @JsonIgnore  // do not expose externally
  @RestResource(exported = false)
  public List<String> voterTokens;

  // This is extremely important! Do not exposed password in toString() !!!
  @Override
  public String toString() {
    return "UserModel{" +
            "id=" + id +
            ", email='" + email + '\'' +
            ", profile=" + profile +
            '}';
  }
}
