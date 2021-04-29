package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.LawRepo;
import org.doogie.liquido.model.LawModel;
import org.springframework.beans.factory.annotation.Autowired;

//@JsonComponent // => I cannot add this, because then these Entity Deserializers are used everywhere, e.g. in @RepositoryRestResource calls
// So therefor I have to manually configure these deserializers where they are needed with @JsonDeserialize annotation
// https://www.baeldung.com/spring-boot-jsoncomponent
public class LawModelDeserializer extends EntityDeserializer<LawModel> {

	@Autowired
	public LawModelDeserializer(LawRepo LawRepo) {
		super(LawRepo, LawModel.class);
	}

}
