package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.LawModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

//@JsonComponent
public class LawModelDeserializer extends EntityDeserializer<LawModel> {

	@Autowired
	public LawModelDeserializer(LawRepo LawRepo) {
		super(LawRepo, LawModel.class);
	}

}
