package org.doogie.liquido.model;

import lombok.Data;

import javax.persistence.Embeddable;

/**
 * Some more data about a user. Subobject of {@link UserModel}
 */
@Data
@Embeddable
public class UserProfileModel {
  String name;
  String website;
  String picture;
  /** Users mobile phone number. Needed for login via SMS code */
  String mobilePhone;
}
