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

import org.springframework.validation.Errors;

/**
 * Thrown by a mutation when its input breaks one or more validation rules. It is an
 * unchecked exception, so the transaction of the mutation rolls back.
 */
class InvalidInputException extends RuntimeException {

	private final transient Errors errors;

	InvalidInputException(Errors errors) {
		super(errors.toString());
		this.errors = errors;
	}

	Errors getErrors() {
		return this.errors;
	}

}
