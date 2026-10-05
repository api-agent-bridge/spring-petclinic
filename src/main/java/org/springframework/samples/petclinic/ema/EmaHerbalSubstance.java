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

import java.time.LocalDate;
import java.util.List;

/**
 * A herbal substance that EMA's Committee on Herbal Medicinal Products has assessed or
 * plans to assess. This is the <code>EmaHerbalSubstance</code> type of the GraphQL
 * schema, read from EMA's report
 * <code>medicines-output-herbal_medicines-report-output-json_en</code>. The schema
 * describes each field.
 */
record EmaHerbalSubstance(String latinName, Boolean combination, String englishName, List<String> botanicalNames,
		List<String> therapeuticAreas, String status, List<String> outcomes, String additionalInformation,
		LocalDate inventoryDate, LocalDate priorityListDate, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaHerbalSubstance from(EmaRecord row) {
		return new EmaHerbalSubstance(row.text("latin_name"), row.yes("combination"), row.text("english_common_name"),
				row.list("botanical_name"), row.list("therapeutic_area"), row.text("status"),
				row.list("outcome_of_european_assessment"), row.text("additional_information"),
				row.date("date_added_to_the_inventory"), row.date("date_added_to_the_priority_list"),
				row.date("first_published_date"), row.date("last_updated_date"), row.text("herbal_medicine_url"));
	}

	/**
	 * One page of substances, as the <code>EmaHerbalSubstancePage</code> type.
	 */
	record Page(List<EmaHerbalSubstance> substances, int page, int totalPages, int totalSubstances) {

	}

}
