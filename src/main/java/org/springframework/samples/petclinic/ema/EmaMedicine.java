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
 * A medicine that EMA has evaluated for the EU market, for humans or for animals. This is
 * the <code>EmaMedicine</code> type of the GraphQL schema, read from EMA's report
 * <code>medicines-output-medicines_json-report_en</code>. The schema describes each
 * field.
 */
record EmaMedicine(EmaCategory category, String name, String productNumber, String status, String opinionStatus,
		String latestProcedure, List<String> commonNames, List<String> activeSubstances, List<String> therapeuticAreas,
		List<String> targetSpecies, Boolean patientSafety, List<String> atcCodes, String atcVetCode,
		List<String> pharmacotherapeuticGroups, List<String> veterinaryPharmacotherapeuticGroups,
		String therapeuticIndication, Boolean acceleratedAssessment, Boolean additionalMonitoring,
		Boolean advancedTherapy, Boolean biosimilar, Boolean conditionalApproval, Boolean exceptionalCircumstances,
		Boolean generic, Boolean orphanMedicine, Boolean primePriorityMedicine, String marketingAuthorisationHolder,
		LocalDate commissionDecisionDate, LocalDate rollingReviewStartDate, LocalDate evaluationStartDate,
		LocalDate opinionDate, LocalDate applicationWithdrawalDate, LocalDate authorisationDate, LocalDate refusalDate,
		LocalDate authorisationEndDate, LocalDate suspensionDate, Integer revisionNumber, LocalDate firstPublishedDate,
		LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaMedicine from(EmaRecord row) {
		return new EmaMedicine(row.category("category"), row.text("name_of_medicine"), row.text("ema_product_number"),
				row.text("medicine_status"), row.text("opinion_status"),
				row.text("latest_procedure_affecting_product_information"),
				row.list("international_non_proprietary_name_common_name"), row.list("active_substance"),
				row.list("therapeutic_area_mesh"), row.list("species_veterinary"), row.yes("patient_safety"),
				row.list("atc_code_human"), row.text("atcvet_code_veterinary"),
				row.list("pharmacotherapeutic_group_human"), row.list("pharmacotherapeutic_group_veterinary"),
				row.text("therapeutic_indication"), row.yes("accelerated_assessment"), row.yes("additional_monitoring"),
				row.yes("advanced_therapy"), row.yes("biosimilar"), row.yes("conditional_approval"),
				row.yes("exceptional_circumstances"), row.yes("generic"), row.yes("orphan_medicine"),
				row.yes("prime_priority_medicine"), row.text("marketing_authorisation_developer_applicant_holder"),
				row.date("european_commission_decision_date"), row.date("start_of_rolling_review_date"),
				row.date("start_of_evaluation_date"), row.date("opinion_adopted_date"),
				row.date("withdrawal_of_application_date"), row.date("marketing_authorisation_date"),
				row.date("refusal_of_marketing_authorisation_date"),
				row.date("withdrawal_expiry_revocation_lapse_of_marketing_authorisation_date"),
				row.date("suspension_of_marketing_authorisation_date"), row.integer("revision_number"),
				row.date("first_published_date"), row.date("last_updated_date"), row.text("medicine_url"));
	}

	/**
	 * One page of medicines, as the <code>EmaMedicinePage</code> type.
	 */
	record Page(List<EmaMedicine> medicines, int page, int totalPages, int totalMedicines) {

	}

}
