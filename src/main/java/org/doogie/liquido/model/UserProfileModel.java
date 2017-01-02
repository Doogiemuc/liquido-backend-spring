package org.doogie.liquido.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Embeddable;

/**
 * Some more data about a user
 */
@Data
@Embeddable
public class UserProfileModel {
  String name;
  String website;
  String picture;
}
