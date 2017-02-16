package org.doogie.liquido.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.hateoas.Identifiable;

import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  //BUGFIX: http://stackoverflow.com/questions/39854429/bootstraping-with-vue-2-router-and-vue-loader
public abstract class BaseModel implements Identifiable<Long> {

  @CreatedDate
  @NotNull
  //  I am not going to use joda time although it is cool   @Type(type="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
  public Date createdAt = new Date();

  @LastModifiedDate
  @NotNull
  public Date updatedAt = new Date();

  //I do not need   @LastModifiedBy
  // @CreatedBy can be added on demand

  /**
   * expose the internal DB ID of our objects. Actually the client gets URIs for identifying entitires.
   * But when inlining a child entity via a projection, this UID makes things much easier for the web client.
   * @return the internal ID as a String value

  public String getURI() {
    return getId().toString();
  }
  */
}