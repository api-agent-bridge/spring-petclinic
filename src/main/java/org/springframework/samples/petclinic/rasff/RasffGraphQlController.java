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

import java.util.Comparator;
import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.rasff.RasffApi.SearchResult;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Answers the <code>rasff*</code> queries of the GraphQL schema from RASFF Window. Every
 * query costs one call. The search pages on the RASFF side, so a page of the schema is a
 * page of RASFF.
 */
@Controller
class RasffGraphQlController {

	static final String RASFF = "RASFF Window";

	private static final Comparator<RasffTerm> BY_DESCRIPTION = Comparator.comparing(RasffTerm::description,
			String.CASE_INSENSITIVE_ORDER);

	private final RasffApi rasff;

	RasffGraphQlController(RasffApi rasff) {
		this.rasff = rasff;
	}

	@QueryMapping
	RasffNotificationSummary.Page rasffNotifications(@Argument RasffNotificationFilter filter, @Argument int page,
			@Argument int size) {
		int pageNumber = ResultPages.pageNumber(page);
		int pageSize = ResultPages.pageSize(size);
		RasffNotificationFilter filters = (filter != null) ? filter : RasffNotificationFilter.NONE;
		SearchResult result = RestUpstreams.call(RASFF,
				() -> this.rasff.search(filters.toRequest(pageNumber, pageSize)));
		List<RasffNotificationSummary> notifications = (result.notifications() != null) ? result.notifications()
				: List.of();
		return new RasffNotificationSummary.Page(notifications, pageNumber, result.totalPages(),
				result.totalElements());
	}

	@QueryMapping
	RasffNotification rasffNotification(@Argument String id) {
		long notificationId;
		try {
			notificationId = Long.parseLong(id.strip());
		}
		catch (NumberFormatException ex) {
			// RASFF numbers its notifications, so this id cannot match one
			return null;
		}
		try {
			return this.rasff.notification(notificationId);
		}
		catch (HttpClientErrorException.Unauthorized ex) {
			// RASFF answers 401 for an id it does not publish, including one that does
			// not exist
			return null;
		}
		catch (RestClientException ex) {
			throw new UpstreamException(RASFF, ex);
		}
	}

	@QueryMapping
	List<RasffTerm> rasffProductCategories() {
		return terms("productCategory");
	}

	@QueryMapping
	List<RasffTerm> rasffProductTypes() {
		return terms("productType");
	}

	@QueryMapping
	List<RasffTerm> rasffHazardCategories() {
		return terms("hazardCategory");
	}

	@QueryMapping
	List<RasffTerm> rasffRiskDecisions() {
		return terms("riskDecision");
	}

	@QueryMapping
	List<RasffTerm> rasffNotificationClassifications() {
		return terms("notificationClassification");
	}

	@QueryMapping
	List<RasffTerm> rasffNotificationBases() {
		return terms("notificationBasis");
	}

	@QueryMapping
	List<RasffTerm> rasffActionsTaken() {
		return terms("actionTaken");
	}

	@QueryMapping
	List<RasffTerm> rasffNotificationStatuses() {
		return terms("notificationStatus");
	}

	@QueryMapping
	List<RasffCountry> rasffCountries() {
		return RestUpstreams.call(RASFF, this.rasff::countries)
			.countries()
			.stream()
			.sorted(Comparator.comparing(RasffCountry::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	@QueryMapping
	List<RasffMember> rasffNetworkMembers() {
		return RestUpstreams.call(RASFF, this.rasff::members)
			.members()
			.stream()
			.sorted(Comparator.comparing(RasffMember::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	/**
	 * Returns a list of terms, sorted by description. RASFF wraps each list in an object
	 * with one key, which differs from list to list.
	 */
	private List<RasffTerm> terms(String list) {
		return RestUpstreams.call(RASFF, () -> this.rasff.terms(list))
			.values()
			.stream()
			.flatMap(List::stream)
			.sorted(BY_DESCRIPTION)
			.toList();
	}

}
