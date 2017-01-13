package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.hateoas.Identifiable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Base class for all our models.
 *
 * Adapted from: https://blog.countableset.com/2014/03/08/auditing-spring-data-jpa-java-config/
 */
@MappedSuperclass
@Data
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseModel  implements Identifiable<Long> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @JsonIgnore
  private final Long id;

  @CreatedDate
  @NotNull
  //  I am not going to use joda time although it is cool   @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  private Date createdAt = new Date();

  @LastModifiedDate
  @NotNull
  private Date updatedAt = new Date();

  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn
  @NotNull
  private UserModel createdBy;

  //I do not need   @LastModifiedBy

}