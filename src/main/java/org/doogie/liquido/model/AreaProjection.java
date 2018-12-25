package org.doogie.liquido.model;

import org.springframework.data.rest.core.config.Projection;

import java.util.Date;

//Implementation note: If you have a projection interface, then this projection will be used when requested
//If you want to always inline a field or value (in any search or get request, then you can create a getter in the model.
// eg.  AreaModel.getCreator() { return getCreatedBy() }  would always inline the "creator" in any JSON of an Area

@Projection(name = "areaProjection", types = { AreaModel.class })
public interface AreaProjection {
  long getId();
  String getTitle();
  String getDescription();
  Date getCreatedAt();
  Date getUpdatedAt();
  UserModel getCreatedBy();
}
