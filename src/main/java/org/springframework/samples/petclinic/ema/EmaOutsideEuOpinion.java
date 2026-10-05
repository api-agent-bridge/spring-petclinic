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
 * An opinion of EMA on a medicine meant for use outside the EU, under the EU-M4all
 * procedure. This is the <code>EmaOutsideEuOpinion</code> type of the GraphQL schema,
 * read from EMA's report <code>medicine-use-outside-eu-output-json-report_en</code>. The
 * schema describes each field.
 */
record EmaOutsideEuOpinion(String name, String opinionNumber, String opinionStatus, List<String> activeSubstances,
		List<String> commonNames, List<String> therapeuticAreas, String atcCode, String latestProcedure,
		String opinionHolder, List<String> pharmacotherapeuticGroups, String therapeuticIndication,
		LocalDate opinionDate, LocalDate outcomeDate, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaOutsideEuOpinion from(EmaRecord row) {
		return new EmaOutsideEuOpinion(row.text("name_of_medicine"), row.text("ema_opinion_number"),
				row.text("ema_opinion_status"), row.list("active_substance"),
				row.list("international_non_proprietary_name_inn_common_name"), row.list("therapeutic_area_mesh"),
				row.text("atc_code_human"), row.text("latest_procedure_affecting_product_information"),
				row.text("opinion_holder"), row.list("pharmacotherapeutic_group_human"),
				row.text("therapeutic_indication"), row.date("date_of_opinion"), row.date("date_of_outcome"),
				row.date("first_published_date"), row.date("last_updated_date"),
				row.text("opinion_on_medicines_for_use_outside_eu_url"));
	}

	/**
	 * One page of opinions, as the <code>EmaOutsideEuOpinionPage</code> type.
	 */
	record Page(List<EmaOutsideEuOpinion> opinions, int page, int totalPages, int totalOpinions) {

	}

}
