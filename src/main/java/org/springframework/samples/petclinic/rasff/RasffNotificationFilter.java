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
package org.springframework.samples.petclinic.rasff;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.samples.petclinic.rasff.RasffApi.Parameters;
import org.springframework.samples.petclinic.rasff.RasffApi.SearchRequest;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;

/**
 * The filters of a notification search, as the <code>RasffNotificationFilter</code> input
 * of the GraphQL schema. Every filter is optional, and the filters a search uses must all
 * match.
 */
record RasffNotificationFilter(String subject, String reference, LocalDate validatedFrom, LocalDate validatedTo,
		List<String> productCategoryIds, List<String> productTypeIds, List<String> hazardCategoryIds,
		List<String> riskDecisionIds, List<String> classificationIds, List<String> basisIds,
		List<String> actionTakenIds, List<String> statusIds, List<String> notifyingMemberIds,
		List<String> originCountryIds, List<String> distributionCountryIds) {

	static final RasffNotificationFilter NONE = new RasffNotificationFilter(null, null, null, null, null, null, null,
			null, null, null, null, null, null, null, null);

	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

	/**
	 * Builds the body of a search for one page of notifications.
	 */
	SearchRequest toRequest(int page, int size) {
		return new SearchRequest(new Parameters(page, size), blankToNull(this.subject), blankToNull(this.reference),
				(this.validatedFrom != null) ? DAY.format(this.validatedFrom) + " 00:00:00" : null,
				(this.validatedTo != null) ? DAY.format(this.validatedTo) + " 23:59:59" : null,
				ids("productCategoryIds", this.productCategoryIds), ids("productTypeIds", this.productTypeIds),
				ids("hazardCategoryIds", this.hazardCategoryIds), ids("riskDecisionIds", this.riskDecisionIds),
				ids("classificationIds", this.classificationIds), ids("basisIds", this.basisIds),
				ids("actionTakenIds", this.actionTakenIds), ids("statusIds", this.statusIds),
				ids("notifyingMemberIds", this.notifyingMemberIds), ids("originCountryIds", this.originCountryIds),
				ids("distributionCountryIds", this.distributionCountryIds));
	}

	private static String blankToNull(String text) {
		return (text != null && !text.isBlank()) ? text.strip() : null;
	}

	/**
	 * Reads the ids of a filter. RASFF numbers its terms, so an id that is not a number
	 * cannot match any of them.
	 */
	private static List<Long> ids(String filter, List<String> ids) {
		if (ids == null || ids.isEmpty()) {
			return null;
		}
		try {
			return ids.stream().map(String::strip).map(Long::valueOf).toList();
		}
		catch (NumberFormatException ex) {
			throw new InvalidArgumentException(filter + " must hold the ids that the RASFF lists return");
		}
	}

}
