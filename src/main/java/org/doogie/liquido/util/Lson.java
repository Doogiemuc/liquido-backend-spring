package org.doogie.liquido.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Liquido json builder "LSON". Yet another JSON builder with a very nice fluid API.
 */
public class Lson extends HashMap<String, Object> {

	public static ObjectMapper mapper;

	public Lson() {
		mapper = new ObjectMapper();
	}

	/** Factory method.  Lson.builder().putArray("name", someValue).... */
	public static Lson builder() {
		return new Lson();
	}

	/** Factory method - shortcut for very simple JSON:  Lson.builder("key", "value") */
	public static Lson builder(String key, Object value) {
		Lson lson = Lson.builder();
		return lson.put(key, value);
	}

	/** Powerfull fluid api to add key=value pairs
	 * <pre>myLson.put("name", someValue).put("key2", anotherValue).put("key3.neested.child.key", valueObj)...</pre>
	 *
	 * Tip: value can also be <pre>Collections.singletonMap("attribute", "single value in map")</pre>
	 *
	 * @param path json key or dot separated json path
	 * @param value any java object. Will be serialized with Jackson
	 */
	public Lson put(String path, Object value) {
		int idx = path.indexOf(".");
		if (idx > 0) {
			String key       = path.substring(0, idx);
			String childPath = path.substring(idx+1);
			Lson child = (Lson)this.get(key);
			if (child != null) {
				child.put(childPath, value);
			} else {
				this.put(key, Lson.builder(childPath, value));
			}
		} else {
			super.put(path, value);
		}
		return this;  // return self instance for chaining
	}

	/**
	 * Put a value under a give child element, which must at least be a Map.
	 * Will create the child as Lson under key1 if that key does not exist in the parent Lson yet.
	 * This way you can easily create something like this:
	 * <pre>
	 * {
	 *   key1: {		// child under key1 must at least be a Map
	 *     key2: value
	 *     //other keys might already exist here.
	 *   }
	 * }
	 * </pre>
	 *
	 * @param key1 child key pointing to the Map
	 * @param key2 map key inside this map
	 * @param value value to putArray into the child Map
	 * @throws ClassCastException when there is no Map under key1
	 * @return the parent Lson for chaining
	 */
	public Lson putLsonChild(String key1, String key2, Object value) {
		if (this.containsKey(key1)) {
			Map child = (Map)this.get(key1);
			child.put(key2, value);
		} else {
			super.put(key1, Lson.builder(key2, value));
		}
		return this;
	}

	/**
	 * Add a json attribute with an <b>array of strings</b> as value.
	 * Not any JSON builder I know of has this obvious helper !! :-)
	 * <pre>String validJson = Lson.builder().putArray("jsonArray", "arrayElemOne", "arrayElemTwo", "arrayElemThree").toString();</pre>
	 * @param key attribute name
	 * @param values a list of strings. Can have any length
	 * @return this for chaining of further fluid calls
	 */
	public Lson putArray(String key, String... values) {
		super.put(key, values);
		return this;
	}

	public HttpEntity<String> toHttpEntity() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<>(this.toString(), headers);
		return entity;
	}

	public String toPrettyString() {
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			String indentedJsonStr = mapper.writeValueAsString(this);
			mapper.disable(SerializationFeature.INDENT_OUTPUT);
			return indentedJsonStr;
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Cannot write out JSON: "+e, e);
		}
	}

	public String toString() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Cannot write out JSON: "+e, e);
		}
	}

}
