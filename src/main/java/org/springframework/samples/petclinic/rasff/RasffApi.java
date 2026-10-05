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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The JSON backend of RASFF Window, the public website of the EU's Rapid Alert System for
 * Food and Feed. The website is the only documented client, and the backend can change
 * without notice.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface RasffApi {

	/**
	 * Returns one of the lists of terms that classify notifications. Each list arrives
	 * wrapped in an object with a single key, and the key differs from list to list, for
	 * example <code>hazardCategories</code> for <code>hazardCategory</code>.
	 * @param list the name of the list, for example <code>hazardCategory</code>
	 */
	@GetExchange("/{list}/list/")
	Map<String, List<RasffTerm>> terms(@PathVariable String list);

	@GetExchange("/country/list/")
	Countries countries();

	@GetExchange("/organization/list/")
	Members members();

	/**
	 * Searches the notifications. The page numbers start at 1, and page 0 comes back
	 * empty.
	 */
	@PostExchange(url = "/notification/search/consolidated/", contentType = MediaType.APPLICATION_JSON_VALUE)
	SearchResult search(@RequestBody SearchRequest request);

	/**
	 * Returns one notification. An id that does not exist, or that belongs to a
	 * notification the public may not see, comes back as <code>401 Unauthorized</code>.
	 */
	@GetExchange("/notification/view/id/{id}/")
	RasffNotification notification(@PathVariable long id);

	record Countries(List<RasffCountry> countries) {

	}

	record Members(@JsonProperty("organizations") List<RasffMember> members) {

	}

	/**
	 * The body of a search, as RASFF Window sends it. Each list filters on the ids of one
	 * list of terms, and a null leaves that filter out.
	 *
	 * @param ecValidDateFrom the first day, as <code>dd-MM-yyyy HH:mm:ss</code>
	 * @param ecValidDateTo the last day, in the same pattern
	 * @param notificationType the ids of the product types
	 * @param notifyingCountry the ids of the members of the network
	 * @param originCountry the ids of the countries
	 * @param distributionCountry the ids of the countries
	 */
	record SearchRequest(Parameters parameters, String subject, String notificationReference, String ecValidDateFrom,
			String ecValidDateTo, List<Long> productCategory, List<Long> notificationType, List<Long> hazardCategory,
			List<Long> riskDecision, List<Long> notificationClassification, List<Long> notificationBasis,
			List<Long> actionTaken, List<Long> notificationStatus, List<Long> notifyingCountry,
			List<Long> originCountry, List<Long> distributionCountry) {

	}

	record Parameters(int pageNumber, int itemsPerPage) {

	}

	record SearchResult(List<RasffNotificationSummary> notifications, int totalPages, int totalElements) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>rasff</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.rasff.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "rasff", types = RasffApi.class)
	class Registration {

	}

}
