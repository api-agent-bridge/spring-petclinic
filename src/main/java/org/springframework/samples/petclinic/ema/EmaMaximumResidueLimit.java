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
 * An EMA assessment of the maximum residue limit of a pharmacologically active substance
 * in food from animals treated with veterinary medicines. This is the
 * <code>EmaMaximumResidueLimit</code> type of the GraphQL schema, read from EMA's report
 * <code>medicines-output-maximum_residue_limits-json-report_en</code>. The schema
 * describes each field.
 */
record EmaMaximumResidueLimit(String title, String activeSubstance, List<String> therapeuticClassifications,
		List<String> targetSpecies, LocalDate commissionDecisionDate, String regulationNumber, String regulationUrl,
		LocalDate firstPublishedDate, LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaMaximumResidueLimit from(EmaRecord row) {
		return new EmaMaximumResidueLimit(row.text("title"), row.text("active_substance"),
				row.list("mrl_therapeutic_classification"), row.list("target_species"),
				row.date("date_of_commission_implementing_decision"), row.text("regulation_number"),
				row.text("regulation_url"), row.date("first_published_date"), row.date("last_updated_date"),
				row.text("url"));
	}

	/**
	 * One page of limits, as the <code>EmaMaximumResidueLimitPage</code> type.
	 */
	record Page(List<EmaMaximumResidueLimit> limits, int page, int totalPages, int totalLimits) {

	}

}
