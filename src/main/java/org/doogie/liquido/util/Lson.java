package org.doogie.liquido.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;

/**
 * Yet another JSON builder
 */
public class Lson {

	public static ObjectMapper mapper;

	HashMap<String, Object> map;

	public Lson() {
		this.map = new HashMap<>();
		this.mapper = new ObjectMapper();
	}

	public Lson put(String key, Object value) {
		map.put(key, value);
		return this;
	}

	public String toString() {
		try {
			return mapper.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Cannot write out JSON: "+e, e);
		}
	}

}
