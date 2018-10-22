package org.doogie.liquido.rest.dto;

import lombok.Data;
import lombok.NonNull;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.hateoas.Resource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for assigning a Proxy
 * POST /assignProxy
 * @see org.doogie.liquido.rest.UserRestController#assignProxy(AssignProxyRequest, PersistentEntityResourceAssembler)
 */
@Data
public class AssignProxyRequest {
	/** proxy that the voter want's to assign */
	@NonNull
	UserModel toProxy;

	/** Area of the assignment */
	@NonNull
	AreaModel area;

	/** The voter's own voterToken that MUST hash to a valid checksumModel. */
	@NonNull
	String voterToken;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("AssignProxyRequest[");
		//buf.append("toProxy="+toProxy);
		buf.append("area="+area);
		//do not expose secret voterToken in toString!
		return buf.toString();
	}
}
