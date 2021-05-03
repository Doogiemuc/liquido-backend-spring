package org.doogie.liquido.rest;

import org.springframework.beans.factory.annotation.Value;

public class LiquidoUrlPaths {

	// REST base path from application.properties
	@Value("${spring.data.rest.base-path}")
	public String basePath;

	public static final String GRAPHQL = "/graphql";
	public static final String SUBSCRIPTIONS = "/subscriptions";
	public static final String PLAYGROUND = "/playground";  // GraphQL playground webfrontend
	public static final String VENDOR = "/vendor";          // GraphQL playground Javascript and static files

}
