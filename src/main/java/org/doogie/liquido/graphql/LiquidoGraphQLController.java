package org.doogie.liquido.graphql;

import graphql.*;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * GraphQL controller that handles POST requests to GraphQL endpoint.
 * The LIQUIDO graphql endpoint is located under the versioned URL prefix: <pre>/liquido/v2/graphql</pre>
 */
@Slf4j
@RestController
@RequestMapping("${spring.data.rest.base-path}")
@CrossOrigin(origins = "*")
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
			//The default SimpleDataFetcherExceptionHandler swallows exception and logs the full stack trace. So we register our own more sophisticated exception handler here.
			//See also https://stackoverflow.com/questions/57215323/correct-way-to-return-errors-in-graphql-spqr
			.queryExecutionStrategy(new AsyncExecutionStrategy(exceptionHandler))
			//.mutationExecutionStrategy(new AsyncExecutionStrategy(exceptionHandler))
			.build();

		// Print the graphQL schema that was created by graphql-spqr
		if (log.isDebugEnabled()) {
			String graphQLschema = new SchemaPrinter().print(schema);
			log.debug(graphQLschema);
		}

	}

	/**
	 * HTTP endpoint for graphQL queries. The /graphql endpoint itself is public.
	 * But most GraphQL query resolver will need an authenticated user (via JWT)
	 *
	 * @param body request body with GraphQL {query: "..."}
	 * @param request the raw HttpServletRequest (POST)
	 * @return The Execution of the GraphQL query: { data:{}, errors: [] }
	 * @throws LiquidoException for GraphQL Syntax error, unauthorized or any other exception
	 */
	@PostMapping(value = LiquidoUrlPaths.GRAPHQL)
	public ExecutionResult execute(@RequestBody Map<String, Object> body, HttpServletRequest request) throws LiquidoException {
		//BUGFIX: Map<String, Object> instead of Map<String, String>   https://github.com/vuejs/vue-apollo/issues/387

		// The actual graphQL-query is a string in GraphQL syntax. This is not JSON.
		// The graphQL query or mutation is wrapped in a "query" field so that the request body is valid JSON.
		Map<String, Object> variables = body.get("variables") != null ? (Map)body.get("variables") : new HashMap<>();
		ExecutionResult result = graphQL.execute(ExecutionInput.newExecutionInput()
			.query((String) body.get("query"))
			.operationName((String) body.get("operationName"))
			.variables(variables)  // must not pass null
			.context(request)
			.build());


		// graphql-java swallows exceptions. Instead a list of errors is returned in result. But result.getData() would just be <null>.
		// So we have to unwrap the errors here, because we want to return a meaningful LiquidoException to our client and not just null.
		//TODO: As specified GraphQL DOES return errors like this. HTTP 200 but with error array.  (I personally don't like it! I prefer HTTP error codes). Let's see if we need to adapt....
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
			throw new LiquidoException(LiquidoException.Errors.GRAPHQL_ERROR, msg, ex, Collections.singletonMap("error-message", err.getMessage()));   // Be careful to not expose secrets the client.
		}

		return result;  // data: {}, errors: []
	}



	/**
	 * Configure Spring Web Security to allow POST requests to GraphQL
	 *
	 * More details here: https://dimitr.im/graphql-spring-security
	 */
	@Configuration
	@Order(31)   // MUST be smaller than 100  to be first!
	public class GraphQlSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
		@Value("${spring.data.rest.base-path}")
		String basePath;

		@Autowired
		Environment springEnv;

		/**
		 * Make the GraphQL endpoint reachable via anonymous POST, without CSRF.
		 * And make the resources for GraphQL-playground available.
		 * @param http HttpSecurity to configure
		 * @throws Exception
		 */
		protected void configure(HttpSecurity http) throws Exception {
			log.info("Configuring WebSecurity for GraphQL API endpoint " + basePath + LiquidoUrlPaths.GRAPHQL + " in env=" + Arrays.toString(springEnv.getActiveProfiles()));

			OrRequestMatcher allowedGraphQlRequests = new OrRequestMatcher(
				new RegexRequestMatcher(basePath + LiquidoUrlPaths.GRAPHQL, HttpMethod.POST.name(), true),
				new RegexRequestMatcher(basePath + LiquidoUrlPaths.SUBSCRIPTIONS, HttpMethod.GET.name()),
				new AntPathRequestMatcher(LiquidoUrlPaths.PLAYGROUND, HttpMethod.GET.name()),  // no base path
				new AntPathRequestMatcher(LiquidoUrlPaths.VENDOR+"/**", HttpMethod.GET.name())
			);



			http.requestMatcher(allowedGraphQlRequests)  		// MUST limit this  to only these URLs !
				.authorizeRequests()
					.anyRequest().permitAll().and().csrf().disable().cors().disable();
			/*
					.antMatchers(basePath + LiquidoUrlPaths.GRAPHQL).permitAll().and().csrf().disable()
				.authorizeRequests()
					.antMatchers(basePath + LiquidoUrlPaths.SUBSCRIPTIONS).permitAll()
					.antMatchers(LiquidoUrlPaths.PLAYGROUND).permitAll()
					.antMatchers(LiquidoUrlPaths.VENDOR + "/**").permitAll();

			 */

		}

	}



}
