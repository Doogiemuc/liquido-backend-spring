package org.doogie.liquido.graphql;

import graphql.*;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
import org.doogie.liquido.services.TeamService;
import org.doogie.liquido.util.Lson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * GraphQL controller that handles POST requests to /graphql endpoint
 */
@Slf4j
@RestController
public class LiquidoGraphQLController {

	private final GraphQL graphQL;

	@Autowired
	public LiquidoGraphQLController(TeamService teamService, LiquidoGraphQLExceptionHandler exceptionHandler) {
		GraphQLSchema schema = new GraphQLSchemaGenerator()
			.withBasePackages("org.doogie.liquido") 			//not mandatory but strongly recommended to set your "root" packages
			.withOperationsFromSingleton(teamService) 		//register the service
			.generate();

		this.graphQL = new GraphQL
			.Builder(schema)
			 .mutationExecutionStrategy(new AsyncExecutionStrategy(exceptionHandler))
			.build();

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

	//TODO: custom exception handler with better logging
	// https://stackoverflow.com/questions/57215323/correct-way-to-return-errors-in-graphql-spqr

	@PostMapping(value = "/graphql")
	public Map<String,Object> execute(@RequestBody @NotNull Map<String, String> request, HttpServletRequest raw) throws LiquidoException {
		ExecutionResult result = graphQL.execute(request.get("query"));

		// GraphQL swallows exceptions. So we have to unwrap them here
		if (result.getErrors() != null && result.getErrors().size() > 0) {
			GraphQLError err = result.getErrors().get(0);
			String msg = err.getMessage();
			Throwable ex = null;
			if (err instanceof ExceptionWhileDataFetching) {
				ex = ((ExceptionWhileDataFetching)err).getException();
				if (ex instanceof LiquidoException) throw (LiquidoException)ex;
			}
			throw new LiquidoException(LiquidoException.Errors.INTERNAL_ERROR, msg, ex);
		}

		return result.getData();
	}


}
