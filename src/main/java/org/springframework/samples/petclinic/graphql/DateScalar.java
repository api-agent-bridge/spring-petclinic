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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import graphql.schema.idl.RuntimeWiring;

import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

/**
 * Registers the <code>Date</code> scalar of the schema, which carries a {@link LocalDate}
 * as an ISO-8601 string such as <code>2013-01-04</code>.
 */
@Component
class DateScalar implements RuntimeWiringConfigurer {

	private static final GraphQLScalarType DATE = GraphQLScalarType.newScalar()
		.name("Date")
		.description("A calendar date in ISO-8601 format")
		.coercing(new LocalDateCoercing())
		.build();

	@Override
	public void configure(RuntimeWiring.Builder builder) {
		builder.scalar(DATE);
	}

	private static final class LocalDateCoercing implements Coercing<LocalDate, String> {

		@Override
		public String serialize(Object dataFetcherResult, GraphQLContext context, Locale locale) {
			if (dataFetcherResult instanceof LocalDate date) {
				return date.toString();
			}
			throw new CoercingSerializeException("Expected a LocalDate but got " + dataFetcherResult);
		}

		@Override
		public LocalDate parseValue(Object input, GraphQLContext context, Locale locale) {
			try {
				return LocalDate.parse(String.valueOf(input));
			}
			catch (DateTimeParseException ex) {
				throw new CoercingParseValueException("Expected a date such as 2013-01-04 but got " + input, ex);
			}
		}

		@Override
		public LocalDate parseLiteral(Value<?> input, CoercedVariables variables, GraphQLContext context,
				Locale locale) {
			if (input instanceof StringValue text) {
				try {
					return LocalDate.parse(text.getValue());
				}
				catch (DateTimeParseException ex) {
					throw new CoercingParseLiteralException(
							"Expected a date such as 2013-01-04 but got " + text.getValue(), ex);
				}
			}
			throw new CoercingParseLiteralException("Expected a date as a string but got " + input);
		}

	}

}
