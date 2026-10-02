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

import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.execution.instrumentation.Instrumentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Limits how much one GraphQL request can ask for. A client chooses the fields of a
 * query, and can repeat a field under many aliases, so the server sets the upper bound.
 */
@Configuration(proxyBeanMethods = false)
class QueryLimitsConfiguration {

	/**
	 * Refuses a query that selects more fields than the limit, before any of it runs.
	 * Every field counts as one, at every level of the query.
	 */
	@Bean
	Instrumentation maxQueryComplexity(@Value("${petclinic.graphql.max-query-complexity}") int maxComplexity) {
		return new MaxQueryComplexityInstrumentation(maxComplexity);
	}

}
