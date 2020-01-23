package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.PollRepo;
import org.doogie.liquido.model.PollModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

//@JsonComponent    When I set this, then create poll does not work anymore..  Instead user @JsonDeserialize(using=PollModelDeserializer.class)   this way the client can pass URIs e.g. "poll": "/polls/1"
public class PollModelDeserializer extends EntityDeserializer<PollModel> {

	@Autowired
	public PollModelDeserializer(PollRepo PollRepo) {
		super(PollRepo, PollModel.class);
	}

}
