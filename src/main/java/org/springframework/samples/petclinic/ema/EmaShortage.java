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
 * A shortage of a medicine in the EU that EMA monitors. This is the
 * <code>EmaShortage</code> type of the GraphQL schema, read from EMA's report
 * <code>shortages-output-json-report_en</code>. The schema describes each field.
 */
record EmaShortage(EmaCategory category, String medicine, String status, List<String> commonNames,
		List<String> therapeuticAreas, List<String> pharmaceuticalForms, List<String> strengths, String alternatives,
		LocalDate startDate, LocalDate expectedResolutionDate, String expectedResolution, LocalDate firstPublishedDate,
		LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaShortage from(EmaRecord row) {
		return new EmaShortage(row.category("category"), row.text("medicine_affected"),
				row.text("supply_shortage_status"), row.list("international_non_proprietary_name_inn_or_common_name"),
				row.list("therapeutic_area_mesh"), row.list("pharmaceutical_forms_affected"),
				row.list("strengths_affected"), row.text("availability_of_alternatives"),
				row.date("start_of_shortage_date"), row.date("expected_resolution_date"),
				row.text("expected_resolution"), row.date("first_published_date"), row.date("last_updated_date"),
				row.text("shortage_url"));
	}

	/**
	 * One page of shortages, as the <code>EmaShortagePage</code> type.
	 */
	record Page(List<EmaShortage> shortages, int page, int totalPages, int totalShortages) {

	}

}
