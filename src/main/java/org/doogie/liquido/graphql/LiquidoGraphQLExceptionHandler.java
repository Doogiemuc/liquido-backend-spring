package org.doogie.liquido.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.*;
import graphql.language.SourceLocation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.doogie.liquido.services.LiquidoException;
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

	@SneakyThrows
	@Override
	public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters params) {
		Throwable exception = params.getException();
		SourceLocation sourceLocation = params.getSourceLocation();
		ExecutionPath path = params.getPath();
		ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(path, exception, sourceLocation);

		//This is was the SimpleDataFetcherExceptionHandler would log:   log.warn("GraphQL Error"+error.getMessage(), exception);
		Lson lson = new Lson(error.toSpecification());
		log.debug("GraphQL Error:\n"+lson.toPrettyString());

		/*
		//TODO: should I throw instead? => then let Spring handle the HTTP response incl. status?
		if (exception instanceof LiquidoException) {
			throw exception;
		} else {
			throw new LiquidoException(LiquidoException.Errors.INTERNAL_ERROR, lson.toString(), exception);
		}

		 */

		return DataFetcherExceptionHandlerResult.newResult().error(error).build();
	}
}
