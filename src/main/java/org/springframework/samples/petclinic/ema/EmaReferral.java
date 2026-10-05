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
 * A referral: a procedure in which EMA settles a question about the safety or benefit of
 * a medicine or a class of medicines for the whole EU. This is the
 * <code>EmaReferral</code> type of the GraphQL schema, read from EMA's report
 * <code>referrals-output-json-report_en</code>. The schema describes each field.
 */
record EmaReferral(EmaCategory category, String name, List<String> commonNames, String status, Boolean safetyReferral,
		String type, List<String> centrallyAuthorisedMedicines, List<String> nationallyAuthorisedMedicines,
		String medicineClass, String referenceNumber, String decisionMakingModel, String pracDecisionMakingModel,
		String authorisationModel, String pracRecommendation, LocalDate procedureStartDate,
		LocalDate pracRecommendationDate, LocalDate cmdhPositionDate, LocalDate opinionDate,
		LocalDate commissionDecisionDate, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaReferral from(EmaRecord row) {
		return new EmaReferral(row.category("category"), row.text("referral_name"),
				row.list("international_non_proprietary_name_inn_common_name"), row.text("current_status"),
				row.yes("safety_referral"), row.text("referral_type"),
				row.list("associated_names_centrally_authorised_medicines"),
				row.list("associated_names_non_centrally_authorised_medicines"), row.text("class"),
				row.text("reference_number"), row.text("non_prac_decision_making_model"),
				row.text("prac_decision_making_model"), row.text("authorisation_model"),
				row.text("prac_recommendation"), row.date("procedure_start_date"), row.date("prac_recommendation_date"),
				row.date("cmdh_position_date"), row.date("chmp_cvmp_opinion_date"),
				row.date("european_commission_decision_date"), row.date("first_published_date"),
				row.date("last_updated_date"), row.text("referral_url"));
	}

	/**
	 * One page of referrals, as the <code>EmaReferralPage</code> type.
	 */
	record Page(List<EmaReferral> referrals, int page, int totalPages, int totalReferrals) {

	}

}
