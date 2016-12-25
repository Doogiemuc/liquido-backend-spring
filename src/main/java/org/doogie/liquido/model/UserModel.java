package org.doogie.liquido.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

/**
 * One user / voter / citizen
 */
@Data
@Document(collection = "users")
public class UserModel {
  @Id
  public String id;

  @NotNull
  @NotEmpty
  public String email;

  @NotNull
  @NotEmpty
  @Getter(AccessLevel.NONE)
  @RestResource(exported = false)   // private: never exposed via REST!
  private String passwordHash;

  public Map<String, String> profile;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

  public UserModel(String email, String passwordHash, Map<String, String> profile) {
    this.email = email;
    this.passwordHash = passwordHash;
    this.profile = profile;
  }

}
