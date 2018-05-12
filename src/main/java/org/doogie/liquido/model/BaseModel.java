package org.doogie.liquido.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.hateoas.Identifiable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Base class for all our models. Automatic handling of CreatedAt and UpdatedAt.
 *
 * Adapted from: https://blog.countableset.com/2014/03/08/auditing-spring-data-jpa-java-config/
 */
@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of="id")
//@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public abstract class BaseModel implements Identifiable<Long> {
  /**
   * Internal ID of this domain object
   * Sprint Data REST will use this in its generated URLs, e.g. GET /laws/ID
   */
  @Id
  @GeneratedValue(strategy= GenerationType.AUTO)
  public Long id;

  @CreatedDate
  @NotNull
  //  I am not going to use joda time although it would be cool   @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  public Date createdAt = new Date();

  @LastModifiedDate
  @NotNull
  public Date updatedAt = new Date();

  //I do not need   @LastModifiedBy

  //No default createdBy here in the BaseModel, because some Models (e.g. BallotModel!!!) do not want it.

  /*
   THIS WAY the ID field could also be exposed for every Model. But I am doing it in the Projections and in the RepositoryRestConfigurer
  public String getID() {
    return getId().toString();
  }
  */
}