package org.doogie.liquido.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

/**
 * Yet another JSON builder. With fluid API.
 */
public class Lson extends HashMap<String, Object> {

	public static ObjectMapper mapper;

	public Lson() {
		this.mapper = new ObjectMapper();
	}

	/** Factory method.  Lson.create().put("name", someValue).... */
	public static Lson create() {
		return new Lson();
	}

	/** fluid api to add key=value pairs  myLson.put("name", someValue).put("key2", anotherValue)... */
	public Lson put(String key, Object value) {
		super.put(key, value);
		return this;
	}

	/**
	 * Add a json attribute with an <b>array of strings</b> as value.
	 * Not any JSON builder I know of has this obvious helper !! :-)
	 * <pre>String validJson = Lson.create().put("jsonArray", "arrayElemOne", "arrayElemTwo", "arrayElemThree").toString();</pre>
	 * @param key attribute name
	 * @param values a list of strings. Can have any length
	 * @return this for chaining of further fluid calls
	 */
	public Lson put(String key, String... values) {
		super.put(key, values);
		return this;
	}

	public String toString() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Cannot write out JSON: "+e, e);
		}
	}

}
