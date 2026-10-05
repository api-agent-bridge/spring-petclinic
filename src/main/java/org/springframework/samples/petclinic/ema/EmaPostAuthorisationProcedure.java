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
 * A procedure that changes an authorised medicine, such as an extension of its
 * indication, with EMA's opinion on it. This is the
 * <code>EmaPostAuthorisationProcedure</code> type of the GraphQL schema, read from EMA's
 * report <code>medicines-output-post_authorisation_json-report_en</code>. The schema
 * describes each field.
 */
record EmaPostAuthorisationProcedure(EmaCategory category, String name, String productNumber,
		List<String> activeSubstances, List<String> commonNames, List<String> therapeuticAreas, List<String> atcCodes,
		String atcVetCode, List<String> targetSpecies, Boolean acceleratedAssessment, Boolean additionalMonitoring,
		Boolean advancedTherapy, Boolean biosimilar, Boolean conditionalApproval, Boolean exceptionalCircumstances,
		Boolean generic, Boolean orphanMedicine, Boolean primePriorityMedicine, String marketingAuthorisationHolder,
		String procedureStatus, String opinionStatus, LocalDate opinionDate, LocalDate applicationWithdrawalDate,
		LocalDate authorisationDate, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaPostAuthorisationProcedure from(EmaRecord row) {
		return new EmaPostAuthorisationProcedure(row.category("category"), row.text("name_of_medicine"),
				row.text("ema_product_number"), row.list("active_substance"),
				row.list("international_non_proprietary_name_common_name"), row.list("therapeutic_area_mesh"),
				row.list("atc_code_human"), row.text("atcvet_code_veterinary"), row.list("species_veterinary"),
				row.yes("accelerated_assessment"), row.yes("additional_monitoring"), row.yes("advanced_therapy"),
				row.yes("biosimilar"), row.yes("conditional_approval"), row.yes("exceptional_circumstances"),
				row.yes("generic"), row.yes("orphan_medicine"), row.yes("prime_priority_medicine"),
				row.text("marketing_authorisation_developer_applicant_holder"),
				row.text("post_authorisation_procedure_status"), row.text("post_authorisation_opinion_status"),
				row.date("post_authorisation_opinion_date"), row.date("withdrawal_of_application_date"),
				row.date("marketing_authorisation_date"), row.date("first_published_date"),
				row.date("last_updated_date"), row.text("medicine_url"));
	}

	/**
	 * One page of procedures, as the <code>EmaPostAuthorisationProcedurePage</code> type.
	 */
	record Page(List<EmaPostAuthorisationProcedure> procedures, int page, int totalPages, int totalProcedures) {

	}

}
