package org.doogie.liquido.model;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

/**
 * One user / voter / citizen
 */
@Document(collection = "users")
public class UserModel {
  @Id
  private String id;

  @NotNull
  @NotEmpty
  public String email;

  @NotNull
  @NotEmpty
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserModel userModel = (UserModel) o;

    if (!email.equals(userModel.email)) return false;
    if (passwordHash != null ? !passwordHash.equals(userModel.passwordHash) : userModel.passwordHash != null)
      return false;
    return profile != null ? profile.equals(userModel.profile) : userModel.profile == null;

  }

  @Override
  public int hashCode() {
    int result = email.hashCode();
    result = 31 * result + (passwordHash != null ? passwordHash.hashCode() : 0);
    result = 31 * result + (profile != null ? profile.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "UserModel{" +
        "id='" + id + '\'' +
        ", email='" + email + '\'' +
        ", passwordHash='" + passwordHash + '\'' +
        ", profile=" + profile +
        '}';
  }

  public String getEMail() {
    return email;
  }

  public String getId() {
    return id;
  }
}
