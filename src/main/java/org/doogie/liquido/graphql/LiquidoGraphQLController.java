package org.doogie.liquido.graphql;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLException;
import graphql.schema.GraphQLSchema;
import io.leangen.graphql.GraphQLSchemaGenerator;
import org.doogie.liquido.services.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * GraphQL controller that handles POST requests to /graphql endpoint
 */
@RestController
public class LiquidoGraphQLController {

	private final GraphQL graphQL;

	@Autowired
	public LiquidoGraphQLController(TeamService teamService) {
		GraphQLSchema schema = new GraphQLSchemaGenerator()
			.withBasePackages("org.doogie.liquido") 			//not mandatory but strongly recommended to set your "root" packages
			.withOperationsFromSingleton(teamService) 		//register the service
			.generate();
		this.graphQL = new GraphQL.Builder(schema).build();

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

	@PostMapping(value = "/graphql")
	public Map<String,Object> execute(@RequestBody Map<String, String> request, HttpServletRequest raw) throws GraphQLException {
		ExecutionResult result = graphQL.execute(request.get("query"));
		return result.getData();
	}


}
