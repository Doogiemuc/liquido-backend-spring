package org.doogie.liquido.rest.deserializer;

import org.doogie.liquido.datarepos.UserRepo;
import org.doogie.liquido.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;

public class UserModelDeserializer extends EntityDeserializer<UserModel> {

	@Autowired
	public UserModelDeserializer(UserRepo userRepo) {
		super(userRepo, UserModel.class);
	}

}
