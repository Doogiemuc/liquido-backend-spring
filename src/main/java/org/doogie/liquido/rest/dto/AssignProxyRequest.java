package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NonNull;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.deserializer.AreaModelDeserializer;
import org.doogie.liquido.rest.deserializer.UserModelDeserializer;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;

/**
 * DTO for assigning a Proxy
 * POST /assignProxy
 * @see org.doogie.liquido.rest.ProxyRestController#assignProxy(AssignProxyRequest, PersistentEntityResourceAssembler)
 */
@Data
public class AssignProxyRequest {
	/** proxy that the voter want's to assign */
	@NonNull
	@JsonDeserialize(using=UserModelDeserializer.class)   // this way the client can pass URIs e.g. "toProxy": "/liquido/v2/users/1"
	UserModel toProxy;

	/** Area of the assignment */
	@NonNull
	@JsonDeserialize(using=AreaModelDeserializer.class)
	AreaModel area;

	/** The voter's own voterToken that MUST hash to a valid checksumModel. */
	@NonNull
	String voterToken;

	/** Should the delegation be transitive, so that the proxy in turn can forward it */
	boolean transitive;

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
