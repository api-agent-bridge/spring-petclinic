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
package org.springframework.samples.petclinic.ema;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.registry.ImportHttpServices;

/**
 * The JSON reports of the European Medicines Agency (EMA) website. Each report is one
 * file that holds every record of one table of the website, refreshed twice a day. The
 * only operation is to download a whole file.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface EmaApi {

	/**
	 * Downloads one report.
	 * @param report the name of the file without its extension, for example
	 * <code>medicines-output-medicines_json-report_en</code>
	 */
	@GetExchange("/{report}.json")
	Report report(@PathVariable String report);

	/**
	 * A report. Each record maps the names of its columns to their values, which are
	 * texts. Some document records also hold the links to their translations in an
	 * object.
	 */
	record Report(List<Map<String, Object>> data) {

	}

	/**
	 * Registers the interface as an HTTP service in the group <code>ema</code>, whose
	 * base URL and timeouts come from <code>spring.http.serviceclient.ema.*</code>.
	 */
	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "ema", types = EmaApi.class)
	class Registration {

	}

}
