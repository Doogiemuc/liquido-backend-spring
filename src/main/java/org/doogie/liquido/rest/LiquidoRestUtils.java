package org.doogie.liquido.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LiquidoRestUtils {

	@Autowired
	Environment springEnv;

	/**
	 * Quick and dirty hack to get the entity ID from an URI.
	 * There is no other clean way to load a HATEOS entity from its URI.
	 *
	 * @param entityName the name of the entitry in the URI  (plural)
	 * @param uri        a fully qualified uri of a spring data rest entity. (links.self.href)
	 * @return the internal db ID of the entity, i.e. just simply the number at the end of the string.
	 */
	public Long getIdFromURI(String entityName, String uri) {
		String basePath = springEnv.getProperty("spring.data.rest.base-path");
		Pattern regex = Pattern.compile(".*" + basePath + "/" + entityName + "\\/(\\d+)");
		Matcher matcher = regex.matcher(uri);
		if (!matcher.matches()) throw new RuntimeException("Invalid uri");
		String entityId = matcher.group(1);  // the number at the end of the uri
		return Long.valueOf(entityId);
	}
}