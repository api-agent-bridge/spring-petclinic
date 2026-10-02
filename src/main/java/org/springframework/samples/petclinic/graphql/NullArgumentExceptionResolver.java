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

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.NonNullableValueCoercedAsNullException;
import graphql.schema.DataFetchingEnvironment;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Reports a null that a variable carries into a non-null argument as
 * <code>BAD_REQUEST</code>, with a message that names the argument. GraphQL validation
 * accepts a nullable variable for an argument that has a default value, so this null is
 * only found when the query runs, and the client would otherwise get
 * <code>INTERNAL_ERROR</code>.
 */
@Component
class NullArgumentExceptionResolver extends DataFetcherExceptionResolverAdapter {

	@Override
	protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
		if (ex instanceof NonNullableValueCoercedAsNullException) {
			return GraphqlErrorBuilder.newError(env).errorType(ErrorType.BAD_REQUEST).message(ex.getMessage()).build();
		}
		return null;
	}

}
