package org.doogie.liquido.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.AsyncExecutionStrategy;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
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
		LiquidoGraphQLExceptionHandler exceptionHandler
	) {
		// Automatically create GraphQL Schema from graphql-spqr annotations
		GraphQLSchema schema = new GraphQLSchemaGenerator()
			.withBasePackages("org.doogie.liquido") 			//not mandatory but strongly recommended to set your "root" packages
			.withOperationsFromSingleton(teamsGraphQL, TeamsGraphQL.class)
			.withOperationsFromSingleton(pollsGraphQL, PollsGraphQL.class)
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
	 * @param request request body with "query" field.
	 * @param raw the raw HttpServletRequest  (POST)
	 * @return
	 * @throws LiquidoException
	 */
	@PostMapping(value = "/graphql")
	public Map<String,Object> execute(@RequestBody @NotNull Map<String, String> request, HttpServletRequest raw) throws LiquidoException {
		// The actual graphQL query is a string(not JSON!). It is wrapped in a "query" field so that the request body is valid JSON.
		ExecutionResult result = graphQL.execute(request.get("query"));

		// GraphQL swallows exceptions. Instead a list of errors is returned in result. But result.getData() would just be <null>.
		// So we have to unwrap the errors here, because we want to return a meaningful LiquidoExceptions to our client and not just null.
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
			throw new LiquidoException(LiquidoException.Errors.GRAPHQL_ERROR, msg, ex);
		}

		return result.getData();
	}


}
