package org.doogie.liquido.model;

import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * One user / voter / citizen
 */
@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor(suppressConstructorProperties = true)
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "users")
public class UserModel {
  @Id
  @GeneratedValue
  public Long id;

  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String email;

  @NotNull
  @NonNull
  @NotEmpty
  @Getter(AccessLevel.NONE)
  @RestResource(exported = false)   // private: never exposed via REST!
  private String passwordHash;

  @Embedded
  public UserProfileModel profile;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

}
