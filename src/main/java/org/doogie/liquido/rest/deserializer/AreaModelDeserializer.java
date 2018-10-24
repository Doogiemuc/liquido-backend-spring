package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.AreaRepo;
import org.doogie.liquido.model.AreaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

@JsonComponent
public class AreaModelDeserializer extends EntityDeserializer<AreaModel> {

	@Autowired
	public AreaModelDeserializer(AreaRepo areaRepo) {
		super(areaRepo);
	}

}
