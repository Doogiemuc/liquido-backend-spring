package org.doogie.liquido.util;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LiquidoRestUtils {

	/** HTTP Media Type */
	public static final String TEXT_URI_LIST_VALUE = "text/uri-list";
	public static final MediaType TEXT_URI_LIST = new MediaType("text", "uri-list");

	/**
	 * Quick and dirty hack to get the entity ID from an URI.
	 * There is no other clean way to load a HATEOAS entity from its URI.
	 *
	 * If you need the other way round, if you need a link for an entity, then you can use {@link org.springframework.hateoas.server.EntityLinks#linkForItemResource(Class, Object)}}
	 *
	 * @param entityName the name of the entitry in the URI in its plural form. See @RepositoryRestResource(path="..."),  e.g. "users" or "laws"
	 * @param uri        a relative or fully qualified uri of a spring data rest entity. (e.g. "/areas/21/", "/liquido/v2/users/48")
	 * @throws IllegalArgumentException if uri is not a valid uri for the given entity
	 * @return the internal db ID of the entity, i.e. just simply the number at the end of the string.
	 */
	public static Long getIdFromURI(String entityName, String uri) {
		if (entityName == null || uri == null)
			throw new IllegalArgumentException("Cannot getIdFromURI. Need entityName and uri! (entityName="+entityName+", uri="+uri+")");
		// RegEx: non-greedy prefix when passing a fully qualified URI,
		// then optionally the entityName enclosed with / and
		// then a number at the end
		// Java Patterns must match the whole string!
		Pattern regex = Pattern.compile(".*?(\\/" + entityName + "\\/)?(\\d+)");
		Matcher matcher = regex.matcher(uri);
		if (!matcher.matches()) throw new IllegalArgumentException("This does not seem to be an URI for an '"+entityName+"': "+uri);
		String entityId = matcher.group(2);  // the number at the end of the uri
		return Long.valueOf(entityId);
	}

	// The other way round from entity ID to entity href can be done like this:
	// https://docs.spring.io/spring-hateoas/docs/0.25.0.RELEASE/reference/html/#fundamentals.obtaining-links.builder
	// Link areaLink = entityLinks.linkToSingleResource(AreaModel.class, area.getId());

	public static final String PROJECTION = "{?projection}";

	/**
	 * Remove the {?projection} suffix from spring hateoas uris.
	 * @param uri a URI that might end with {?projection} suffix.
	 * @return the uri without that damn suffix
	 */
	public static String cleanURI(String uri) {
		if (uri.endsWith(PROJECTION)) uri = uri.substring(0, uri.length()-PROJECTION.length());
		return uri;
	}

	/**
	 * Clean mobile phone number: Replace everything except plus('+') and number (0-9).
	 * Specifically spaces will be removed.
	 * This is a very simple thing. Have a look at google phone lib for sophisticated phone number parsing
	 * @param mobile a non formatted phone numer
	 * @return the cleaned up phone number
	 */
	public static String cleanMobilephone(String mobile) {
		if (mobile == null) return null;
		return mobile.replaceAll("[^\\+0-9]", "");
	}

	/**
	 * emails a case IN-sensitive. So store and compare them in lowercase
	 * @param email an email address
	 * @return the email in lowercase
	 */
	public static String cleanEmail(String email) {
		if (email == null) return null;
		return email.toLowerCase();
	}
}