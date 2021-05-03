package org.doogie.liquido.graphql;

import graphql.*;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.rest.LiquidoUrlPaths;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;

/**
 * GraphQL controller that handles POST requests to GraphQL endpoint.
 * The LIQUIDO graphql endpoint is located under the versioned URL prefix: <pre>/liquido/v2/graphql</pre>
 */
@Slf4j
@RestController
@RequestMapping("${spring.data.rest.base-path}")
public class LiquidoGraphQLController {

	private final GraphQL graphQL;

	@Autowired
	public LiquidoGraphQLController(
		TeamsGraphQL teamsGraphQL,
		PollsGraphQL pollsGraphQL,
		UserGraphQL userGraphQL,
		LiquidoGraphQLExceptionHandler exceptionHandler
	) {
		// Automatically create GraphQL Schema from graphql-spqr annotations
		GraphQLSchema schema = new GraphQLSchemaGenerator()
			.withBasePackages("org.doogie.liquido") 			//not mandatory but strongly recommended to set your "root" packages
			.withOperationsFromSingleton(teamsGraphQL, TeamsGraphQL.class)
			.withOperationsFromSingleton(pollsGraphQL, PollsGraphQL.class)
			.withOperationsFromSingleton(userGraphQL, UserGraphQL.class)
			.withOutputConverters()
			.generate();

		this.graphQL = new GraphQL.Builder(schema)
			//The default SimpleDataFetcherExceptionHandler  swallows exception and logs the full stack trace. So we register our own more sophisticated exception handler:
			//See also https://stackoverflow.com/questions/57215323/correct-way-to-return-errors-in-graphql-spqr
			.queryExecutionStrategy(new AsyncExecutionStrategy(exceptionHandler))
			//.mutationExecutionStrategy(new AsyncExecutionStrategy(exceptionHandler))
			.build();

		// Print the graphQL schema that was created by graphql-spqr
		//String graphQLschema = new SchemaPrinter().print(schema);
		//log.debug(graphQLschema);

		/*
		  more complex example from this tutorial https://medium.com/@saurabh1226/getting-started-with-graphql-spqr-with-springboot-bb9d232053ec

		GraphQLSchema schema = new GraphQLSchemaGenerator().withResolverBuilders(
			// Resolve by annotations
			new AnnotatedResolverBuilder(),
			// Resolve public methods inside root package
			new PublicResolverBuilder("com.graphql.userPoc"))
			.withOperationsFromSingleton(userResolver, UserResolver.class)
			.withValueMapperFactory(new JacksonValueMapperFactory()).generate();
		graphQL = GraphQL.newGraphQL(schema).build();

		 */
	}

	/**
	 * HTTP endpoint for graphQL queries. The /graphql endpoint itself is public.
	 * But most GraphQL query resolver will need an authenticated user (via JWT)
	 * @param body request body with GraphQL {query: "..."}
	 * @param request the raw HttpServletRequest (POST)
	 * @return The Execution of the GraphQL query: { data:{}, errors: [] }
	 * @throws LiquidoException for GraphQL Syntax error, unauthorized or any other exception
	 */
	@PostMapping(value = LiquidoUrlPaths.GRAPHQL)
	public ExecutionResult execute(@RequestBody Map<String, Object> body, HttpServletRequest request) throws LiquidoException {
		//BUGFIX: Map<String, Object> instead of Map<String, String>   https://github.com/vuejs/vue-apollo/issues/387

		// The actual graphQL-query is a string(not JSON!). It is wrapped in a "query" field so that the request body is valid JSON.
		//ExecutionResult result = graphQL.execute((String) body.get("query"));

		ExecutionResult result = graphQL.execute(ExecutionInput.newExecutionInput()
			.query((String) body.get("query"))
			.operationName((String) body.get("operationName"))
			//.variables(body.get("variables"))
			.context(request)
			.build());

		// graphql-java swallows exceptions. Instead a list of errors is returned in result. But result.getData() would just be <null>.
		// So we have to unwrap the errors here, because we want to return a meaningful LiquidoException to our client and not just null.
		for(GraphQLError err : result.getErrors()) {
			String msg = err.getMessage();
			Throwable ex = null;
			if (err instanceof ExceptionWhileDataFetching) {
				ex = ((ExceptionWhileDataFetching)err).getException();
				if (ex instanceof LiquidoException) throw (LiquidoException)ex;
				if (ex instanceof AccessDeniedException) {
					throw new LiquidoException(LiquidoException.Errors.UNAUTHORIZED, msg, ex);
				}
			}
			// In a graphQL world the request URL is always the same.
			// But our awesome LiquidoException can add the request body to the error response.
			// So the caller knows which of his GraphQL queries or mutations resulted in this error.
			// (will be logged in LiquidoGraphQLExceptionHandler.java)
			throw new LiquidoException(LiquidoException.Errors.GRAPHQL_ERROR, msg, null, body);   // Do not expose inner exception to the client. Msg is enough.
		}

		return result;  // data: {}, errors: []
	}



	/**
	 * Configure Spring Web Security to allow POST requests to GraphQL
	 *
	 * More details here: https://dimitr.im/graphql-spring-security
	 */
	@Configuration
	@Order(2)   // MUST be smaller than 100  to be first!
	public class GraphQlSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
		@Value("${spring.data.rest.base-path}")
		String basePath;

		@Autowired
		Environment springEnv;

		/**
		 * Allow anonymous access to GraphQL API and web resources for GraphQL-playground
     */
		protected void configure(HttpSecurity http) throws Exception {
			log.info("Configuring WebSecurity for GraphQL API endpoint " + basePath + LiquidoUrlPaths.GRAPHQL + " in env=" + Arrays.toString(springEnv.getActiveProfiles()));
			http
				.authorizeRequests()
					.antMatchers(HttpMethod.POST, basePath + LiquidoUrlPaths.GRAPHQL).permitAll().and().csrf().disable()
				.authorizeRequests()
					.antMatchers(basePath + LiquidoUrlPaths.SUBSCRIPTIONS).permitAll()
					.antMatchers(basePath + LiquidoUrlPaths.PLAYGROUND).permitAll()
					.antMatchers(basePath + LiquidoUrlPaths.VENDOR + "/**").permitAll();
		}
	}




}
