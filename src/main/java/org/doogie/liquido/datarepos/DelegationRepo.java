package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.DelegationModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Database abstraction for MongoDB collection "delegations".
 */
public interface DelegationRepo extends MongoRepository<DelegationModel, String>, DelegationRepoCustom {

  List<DelegationModel> findByToId(String proxyId);

}
