/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.graphql;

import java.util.List;
import java.util.Map;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.NonNullableValueCoercedAsNullException;
import graphql.schema.DataFetchingEnvironment;

import org.springframework.context.MessageSource;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

/**
 * Turns the mistakes a client can make into errors the client can act on. An exception
 * that reaches Spring for GraphQL unresolved becomes <code>INTERNAL_ERROR</code> with a
 * generic message, which is right for a fault of the server and hides what a client has
 * to correct.
 * <ul>
 * <li>An input that breaks validation rules gives one <code>BAD_REQUEST</code> error for
 * each field, with the name of the field in the message and in the extensions.</li>
 * <li>An id that does not exist gives <code>NOT_FOUND</code>.</li>
 * <li>A null that a variable carries into a non-null argument gives
 * <code>BAD_REQUEST</code>. GraphQL validation accepts a nullable variable for an
 * argument that has a default value, so this null is only found when the query runs.</li>
 * </ul>
 */
@Component
class ClientErrorExceptionResolver extends DataFetcherExceptionResolverAdapter {

	private final MessageSource messages;

	ClientErrorExceptionResolver(MessageSource messages) {
		this.messages = messages;
	}

	@Override
	protected List<GraphQLError> resolveToMultipleErrors(Throwable ex, DataFetchingEnvironment env) {
		if (ex instanceof InvalidInputException invalidInput) {
			return invalidInput.getErrors()
				.getFieldErrors()
				.stream()
				.map((fieldError) -> badRequest(fieldError, env))
				.toList();
		}
		if (ex instanceof UnknownIdException) {
			return List.of(error(ErrorType.NOT_FOUND, ex.getMessage(), env));
		}
		if (ex instanceof NonNullableValueCoercedAsNullException) {
			return List.of(error(ErrorType.BAD_REQUEST, ex.getMessage(), env));
		}
		return null;
	}

	private GraphQLError badRequest(FieldError fieldError, DataFetchingEnvironment env) {
		// the same message the web form shows next to the field
		String message = this.messages.getMessage(fieldError, env.getLocale());
		return GraphqlErrorBuilder.newError(env)
			.errorType(ErrorType.BAD_REQUEST)
			.message(fieldError.getField() + ": " + message)
			.extensions(Map.of("field", fieldError.getField()))
			.build();
	}

	private static GraphQLError error(ErrorType type, String message, DataFetchingEnvironment env) {
		return GraphqlErrorBuilder.newError(env).errorType(type).message(message).build();
	}

}
