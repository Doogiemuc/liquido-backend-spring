package org.doogie.liquido.model;

import lombok.Data;

import javax.persistence.Column;
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
  //@Column(unique = true)
  String mobilephone;

  /**
   *  Always store mobilephone with '+' and numbers only. No spaces.
   * @param mobile
   */
  public void setMobilephone(String mobile) {
    if (mobile == null) {
      this.mobilephone = "";
    } else {
      this.mobilephone = mobile.replaceAll("[^0-9\\+]", "");
    }
  }
}
