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
 * An orphan designation: EMA's decision that a medicine under development treats a rare
 * disease and can get the incentives for orphan medicines. This is the
 * <code>EmaOrphanDesignation</code> type of the GraphQL schema, read from EMA's report
 * <code>medicines-output-orphan_designations-json-report_en</code>. The schema describes
 * each field.
 */
record EmaOrphanDesignation(List<String> medicineNames, List<String> productNumbers, List<String> activeSubstances,
		LocalDate decisionDate, String intendedUse, String designationNumber, String status,
		LocalDate firstPublishedDate, LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaOrphanDesignation from(EmaRecord row) {
		return new EmaOrphanDesignation(row.list("medicine_name"), row.list("related_ema_product_number"),
				row.list("active_substance"), row.date("date_of_designation_or_refusal"), row.text("intended_use"),
				row.text("eu_designation_number"), row.text("status"), row.date("first_published_date"),
				row.date("last_updated_date"), row.text("orphan_designation_url"));
	}

	/**
	 * One page of designations, as the <code>EmaOrphanDesignationPage</code> type.
	 */
	record Page(List<EmaOrphanDesignation> designations, int page, int totalPages, int totalDesignations) {

	}

}
