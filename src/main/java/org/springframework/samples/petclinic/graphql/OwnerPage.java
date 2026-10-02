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

import org.springframework.data.domain.Page;
import org.springframework.samples.petclinic.owner.Owner;

/**
 * One page of owners, as the <code>OwnerPage</code> type of the GraphQL schema.
 *
 * @param owners the owners on this page
 * @param page the page number, starting at 1
 * @param totalPages the number of pages available
 * @param totalOwners the number of owners across all pages
 */
record OwnerPage(List<Owner> owners, int page, int totalPages, long totalOwners) {

	static OwnerPage of(Page<Owner> page) {
		return new OwnerPage(page.getContent(), page.getNumber() + 1, page.getTotalPages(), page.getTotalElements());
	}

}
