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
 * An EMA decision on a paediatric investigation plan (PIP): the studies a company must
 * run in children, or a waiver from them. This is the
 * <code>EmaPaediatricInvestigationPlan</code> type of the GraphQL schema, read from EMA's
 * report
 * <code>medicines-output-paediatric_investigation_plans-output-json-report_en</code>. The
 * schema describes each field.
 */
record EmaPaediatricInvestigationPlan(String decisionNumber, String pipNumber, List<String> activeSubstances,
		List<String> inventedNames, List<String> therapeuticAreas, List<String> pharmaceuticalForms,
		List<String> conditions, List<String> routesOfAdministration, String decisionType, LocalDate decisionDate,
		String complianceOutcome, LocalDate complianceOpinionDate, String complianceProcedureNumber,
		String publicEnquiriesContact, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaPaediatricInvestigationPlan from(EmaRecord row) {
		return new EmaPaediatricInvestigationPlan(row.text("decision_number"), row.text("pip_number"),
				row.list("active_substance"), row.list("invented_name"), row.list("therapeutic_area"),
				row.list("pharmaceutical_forms"), row.list("condition_indication"),
				row.list("routes_of_administration"), row.text("decision_type"), row.date("decision_date"),
				row.text("compliance_outcome"), row.date("compliance_opinion_date"),
				row.text("compliance_procedure_number"), row.text("contact_for_public_enquiries"),
				row.date("first_published_date"), row.date("last_updated_date"), row.text("pip_url"));
	}

	/**
	 * One page of plans, as the <code>EmaPaediatricInvestigationPlanPage</code> type.
	 */
	record Page(List<EmaPaediatricInvestigationPlan> plans, int page, int totalPages, int totalPlans) {

	}

}
