package org.doogie.liquido.rest.dto;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.doogie.liquido.model.UserModel;

@Data
@RequiredArgsConstructor
public class ProxyMapResponseElem {
	@NonNull
	UserModel directProxy;

	@NonNull
	UserModel topProxy;
}
