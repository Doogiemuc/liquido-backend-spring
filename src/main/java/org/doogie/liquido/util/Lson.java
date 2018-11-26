package org.doogie.liquido.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;

/**
 * Liquido json builder "LSON". Yet another JSON builder with a very nice fluid API.
 */
public class Lson extends HashMap<String, Object> {

	public static ObjectMapper mapper;

	public Lson() {
		this.mapper = new ObjectMapper();
	}

	/** Factory method.  Lson.builder().put("name", someValue).... */
	public static Lson builder() {
		return new Lson();
	}

	/** Factory method - shortcut for very simple JSON:  Lson.builder("key", "value") */
	public static Lson builder(String key, Object value) {
		Lson lson = Lson.builder();
		return lson.put(key, value);
	}

	/** Powerfull fluid api to add key=value pairs
	 * <pre>myLson.put("name", someValue).put("key2", anotherValue).put("some.neested.attribute", "value")...</pre>
	 *
	 * Tip: value can also be <pre>Collections.singletonMap("attribute", "single value in map")</pre>
	 *
	 * @param key json key or may also be dot separated list of neested
	 * @param value any java object that will be serialized with Jackson
	 */
	public Lson put(String key, Object value) {
		/*
		int idx = key.indexOf(".");
		if (idx > 0) {
			String keyPart = key.substring(0, idx);
			Object child = this.get(keyPart);
			String rest = key.substring(idx+1, key.length());
			if (chidlMap  != null) {
				childMap.put(keyPart, value);
			}

			Map childMap = new HashMap();

			super.put(keyPart, rest);
			return this;
		}
		*/
		super.put(key, value);
		return this;
	}

	/**
	 * Add a json attribute with an <b>array of strings</b> as value.
	 * Not any JSON builder I know of has this obvious helper !! :-)
	 * <pre>String validJson = Lson.builder().put("jsonArray", "arrayElemOne", "arrayElemTwo", "arrayElemThree").toString();</pre>
	 * @param key attribute name
	 * @param values a list of strings. Can have any length
	 * @return this for chaining of further fluid calls
	 */
	public Lson put(String key, String... values) {
		super.put(key, values);
		return this;
	}

	public HttpEntity<String> toHttpEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(this.toString(), headers);
		return entity;
	}

	public String toString() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Cannot write out JSON: "+e, e);
		}
	}

}
