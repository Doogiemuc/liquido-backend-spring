package org.doogie.liquido.rest.dto;

import lombok.Data;
import org.doogie.liquido.model.LawModel;

import java.util.Date;
import java.util.Optional;

@Data
public class LawQuery {

	Optional<LawModel.LawStatus> status = Optional.empty();
	Optional<String> searchText = Optional.empty();
	Optional<Date> updatedAfter = Optional.empty();
	Optional<Date> updatedBefore= Optional.empty();
	Optional<String> areaTitle = Optional.empty();
	Optional<String> createdByEmail = Optional.empty();
	Optional<String> supportedByEMail = Optional.empty();
}
