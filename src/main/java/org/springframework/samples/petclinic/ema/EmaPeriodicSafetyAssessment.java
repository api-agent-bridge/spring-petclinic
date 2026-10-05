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
 * A single assessment of periodic safety update reports (PSUSA): EMA's review of the
 * safety data of an active substance across all the medicines that contain it. This is
 * the <code>EmaPeriodicSafetyAssessment</code> type of the GraphQL schema, read from
 * EMA's report
 * <code>medicines-output-periodic_safety_update_report_single_assessments-output-json-report_en</code>.
 * The schema describes each field.
 */
record EmaPeriodicSafetyAssessment(EmaCategory category, List<String> substancesInScope, List<String> activeSubstances,
		List<String> relatedMedicines, String procedureNumber, String regulatoryOutcome, LocalDate firstPublishedDate,
		LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaPeriodicSafetyAssessment from(EmaRecord row) {
		return new EmaPeriodicSafetyAssessment(row.category("category"),
				row.list("active_substances_in_scope_of_procedure"), row.list("active_substance"),
				row.list("related_medicines"), row.text("procedure_number"), row.text("regulatory_outcome"),
				row.date("first_published_date"), row.date("last_updated_date"), row.text("psusa_url"));
	}

	/**
	 * One page of assessments, as the <code>EmaPeriodicSafetyAssessmentPage</code> type.
	 */
	record Page(List<EmaPeriodicSafetyAssessment> assessments, int page, int totalPages, int totalAssessments) {

	}

}
