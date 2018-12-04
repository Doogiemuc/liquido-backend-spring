package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.model.AreaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

//@JsonComponent  //TODO: now this deserializer will be used everywhere  => Which was a problem somewhere. But I don't remember where.
public class AreaModelDeserializer extends EntityDeserializer<AreaModel> {

	@Autowired
	public AreaModelDeserializer(AreaRepo areaRepo) {
		super(areaRepo, AreaModel.class);
	}

}
