package org.doogie.liquido.datarepos;

import org.doogie.liquido.model.TeamModel;
import org.springframework.data.repository.CrudRepository;

public interface TeamRepo extends CrudRepository<TeamModel, Long> {

}
