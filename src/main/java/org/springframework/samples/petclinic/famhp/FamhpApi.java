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
package org.springframework.samples.petclinic.famhp;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The public backend of the medicines database of the Belgian Federal Agency for
 * Medicines and Health Products (FAMHP). The database's website loads its reference data
 * from <code>/api/resources</code>. Its product search needs a session, so this interface
 * covers the reference data only.
 * <p>
 * The labels come in the language of the <code>Accept-Language</code> header, and in
 * Dutch without it.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE, headers = "Accept-Language=en")
interface FamhpApi {

	/**
	 * Returns every entry of the reference data, of all types in one list.
	 */
	@GetExchange("/resources")
	List<Resource> resources();

	/**
	 * One entry of the reference data, as the database sends it.
	 *
	 * @param id the database's identifier
	 * @param type the list the entry belongs to, for example <code>targetSpecies</code>
	 * @param code a short code, empty for some types
	 * @param label the English name
	 * @param usage <code>human</code>, <code>veterinary</code> or both, for the types
	 * that distinguish them
	 * @param riskMinimisation whether a document type holds risk minimisation material
	 * @param sequence the position of a document type in the database's own order
	 */
	record Resource(String id, String type, String code, String label, List<String> usage,
			@JsonProperty("isRMA") Boolean riskMinimisation, int sequence) {
	}

	/**
	 * Registers the interface as an HTTP service in the group <code>famhp</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.famhp.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "famhp", types = FamhpApi.class)
	class Registration {

	}

}
