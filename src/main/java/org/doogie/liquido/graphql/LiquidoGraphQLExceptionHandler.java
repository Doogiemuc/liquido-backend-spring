package org.doogie.liquido.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ResultPath;
import graphql.language.SourceLocation;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.util.Lson;
import org.springframework.stereotype.Component;

/**
 * Better logging than {@link graphql.execution.SimpleDataFetcherExceptionHandler}
 * This class handles general GraphQL errors.
 * Service methods (e.g. @GraphQLMutation) check for their own use case specific errors.
 */
@Slf4j
@Component
public class LiquidoGraphQLExceptionHandler implements DataFetcherExceptionHandler {

	/**
	 * Instead of just dumping the whole stacktrace, we log usefull information.
	 * The "exception" is not necessarily "bad". This is also called for unauthorized (parts of) graphQL queries.
	 * Or for duplicate key exceptions etc.
	 *
	 * @param params contains usefull detail information about the GraphQL exception
	 * @return DataFetcherExceptionHandlerResult that contains the list of errors.
	 */
	@Override
	public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters params) {
		Throwable exception = params.getException();
		SourceLocation sourceLocation = params.getSourceLocation();
		ResultPath path = params.getPath();
		ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(path, exception, sourceLocation);

		//SimpleDataFetcherExceptionHandler logs the whole stacktrace:  log.warn("GraphQL Error"+error.getMessage(), exception); But we can do better
		Lson lson = new Lson(error.toSpecification());
		log.debug("GraphQL exception while data fetching:\n"+lson.toPrettyString());

		return DataFetcherExceptionHandlerResult.newResult().error(error).build();
	}
}
