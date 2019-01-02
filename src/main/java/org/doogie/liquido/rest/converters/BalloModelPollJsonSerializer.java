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
 * I don't need the fully serialized HATEOAS info about a poll in the BallotModel.  Just the href is enough.
 */
public class BalloModelPollJsonSerializer extends StdSerializer<PollModel> {
	protected BalloModelPollJsonSerializer() {
		super(PollModel.class);
	}

	@Autowired
	EntityLinks entityLinks;


	/**
	 * <pre>
	 *     "_links": {    <= key is named in BallotModel
	 *         "poll": {
	 *             "href": "my demo link"
	 *         }
	 *     }
	 * </pre>
	 * @param poll
	 * @param gen
	 * @param provider
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
