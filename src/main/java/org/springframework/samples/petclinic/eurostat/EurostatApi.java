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
package org.springframework.samples.petclinic.eurostat;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The statistics API of Eurostat's dissemination service. One call returns the
 * observations of a dataset that match a filter on each of its dimensions, in the
 * JSON-stat format.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface EurostatApi {

	/**
	 * Returns the observations of a dataset.
	 * @param dataset the code of the dataset, for example <code>prc_hicp_minr</code>
	 * @param filters one value for each dimension to filter on, for example
	 * <code>geo=BE</code>, and the time filters <code>sinceTimePeriod</code>,
	 * <code>untilTimePeriod</code> and <code>lastTimePeriod</code>
	 */
	@GetExchange("/{dataset}?format=JSON&lang=EN")
	Dataset data(@PathVariable String dataset, @RequestParam MultiValueMap<String, String> filters);

	/**
	 * A JSON-stat dataset. The values are numbered in one sequence across all the
	 * dimensions, and the numbers are keys of <code>value</code> and <code>status</code>.
	 * Eurostat leaves out the numbers of the observations it has not published.
	 *
	 * @param label the title of the dataset
	 * @param updated when Eurostat last updated the data, as an ISO-8601 timestamp
	 * @param id the names of the dimensions, in the order of the sequence
	 * @param size the number of categories of each dimension, in the same order
	 * @param dimension the categories of each dimension, by name
	 * @param value the observations, by their number
	 * @param status a flag for some observations, by their number, for example
	 * <code>e</code> for an estimate
	 */
	record Dataset(String label, String updated, List<String> id, List<Integer> size, Map<String, Dimension> dimension,
			Map<String, Double> value, Map<String, String> status) {

	}

	/**
	 * One dimension of a dataset, such as the country or the time.
	 *
	 * @param label the name of the dimension
	 * @param category its categories
	 */
	record Dimension(String label, Category category) {

	}

	/**
	 * The categories of a dimension.
	 *
	 * @param index the position of each category code in the dimension
	 * @param label the name of each category code
	 */
	record Category(Map<String, Integer> index, Map<String, String> label) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>eurostat</code>,
	 * whose base URL and timeouts come from
	 * <code>spring.http.serviceclient.eurostat.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "eurostat", types = EurostatApi.class)
	class Registration {

	}

}
