package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.doogie.liquido.model.AreaModel;
import org.doogie.liquido.model.UserModel;
import org.doogie.liquido.rest.deserializer.UserModelDeserializer;

/**
 * DTO for assigning a Proxy
 * The PUT /my/proxy/{areaId} request body contains JSON with URI links to the entities. These are then deserialized into entity classes
 * by my custom Jackson deserializers
 * @see org.doogie.liquido.rest.ProxyRestController#assignProxy(AreaModel, AssignProxyRequest)
 */
@Data
@NoArgsConstructor  //BUGFIX: Lombok's @Data does NOT contain @NoArgsConstructor
public class AssignProxyRequest {
	/** proxy that the voter want's to assign */
	@NonNull
	@JsonDeserialize(using=UserModelDeserializer.class)   // this way the client can pass URIs e.g. "toProxy": "/users/1"
	UserModel toProxy;

	/** The voter's own voterToken that MUST hash to a valid checksumModel. */
	@NonNull
	String voterToken;

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("AssignProxyRequest[");
		buf.append("toProxy="+toProxy);
		buf.append("]");
		//do not expose secret voterToken in toString!
		return buf.toString();
	}
}
