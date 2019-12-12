package org.doogie.liquido.rest.converters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.doogie.liquido.model.PollModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;

import java.io.IOException;

/**
 * Custom Jackson serializer for the poll link inside a BallotModel.
 * I don't need the fully serialized HATEOAS info about a poll in the BallotModel. Just the href is enough.
 */
public class PollAsLinkJsonSerializer extends StdSerializer<PollModel> {
	@Autowired
	EntityLinks entityLinks;

	protected PollAsLinkJsonSerializer() {
		super(PollModel.class);
	}

	/**
	 * Serialize a PollModel into a HATEOAS _link
	 * <pre>
	 *     "_links": {    <= this json key ("_links") is set in attribute of parent entity
	 *         "poll": {
	 *             "href": "http://domain.tld/api/poll/4711"   <= link to poll entity resource
	 *         }
	 *     }
	 * </pre>
	 * @param poll the poll that we link to
	 * @param gen jackson JsonGenerator that is used to generate JSON further objects and fields during serialization
	 * @param provider (not used)
	 * @throws IOException
	 */
	@Override
	public void serialize(PollModel poll, JsonGenerator gen, SerializerProvider provider) throws IOException {
		Link link = entityLinks.linkToSingleResource(poll);
		gen.writeStartObject();
		gen.writeObjectFieldStart("poll");
		gen.writeObjectField("href", link.getHref());
		gen.writeEndObject();
		gen.writeEndObject();
	}
}
