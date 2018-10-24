package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.PollModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class PollModelDeserializer extends EntityDeserializer<PollModel> {

	@Autowired
	public PollModelDeserializer(PollRepo PollRepo) {
		super(PollRepo);
	}

}
