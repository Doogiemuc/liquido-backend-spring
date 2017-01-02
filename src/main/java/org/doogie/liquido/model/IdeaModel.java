package org.doogie.liquido.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@RequiredArgsConstructor
@EntityListeners(AuditingEntityListener.class)  // this is necessary so that UpdatedAt and CreatedAt are handled.
@Table(name = "ideas")
public class IdeaModel {
  @Id
  @GeneratedValue
  public Long id;

  @NotNull
  @NonNull
  @NotEmpty
  @Column(unique = true)
  public String title;

  @NotNull
  @NonNull
  @NotEmpty
  public String description;

  @NonNull
  @OneToOne
  AreaModel area;

  /** list of users that support this idea */
  @ManyToMany(fetch = FetchType.EAGER)    //BUGFIX: EAGER loading is neseccary for testCreateIdeaWithAllRefs to work!
  List<UserModel> supporters;  // need list of all supporters so that no one is allowed to vote twice

  //TODO: configure createBy
  // http://docs.spring.io/spring-data/jpa/docs/current/reference/html/index.html#auditing.auditor-aware
  // https://blog.countableset.com/2014/03/08/auditing-spring-data-jpa-java-config/    nice example
  @CreatedBy
  @NonNull
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  public UserModel createdBy;

  @LastModifiedDate
  public Date updatedAt;

  @CreatedDate
  public Date createdAt;

}
