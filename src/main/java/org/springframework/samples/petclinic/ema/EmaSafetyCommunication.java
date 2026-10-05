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
 * A direct healthcare professional communication (DHPC): a letter that tells doctors,
 * pharmacists or veterinarians about a new safety issue of a medicine. This is the
 * <code>EmaSafetyCommunication</code> type of the GraphQL schema, read from EMA's report
 * <code>dhpc-output-json-report_en</code>. The schema describes each field.
 */
record EmaSafetyCommunication(EmaCategory category, String medicine, List<String> procedureNumbers,
		List<String> activeSubstances, List<String> types, String regulatoryOutcome, String referralName,
		List<String> atcCodes, String atcVetCode, List<String> therapeuticAreas, List<String> targetSpecies,
		List<String> otherMedicines, LocalDate disseminationDate, LocalDate firstPublishedDate,
		LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaSafetyCommunication from(EmaRecord row) {
		return new EmaSafetyCommunication(row.category("category"), row.text("name_of_medicine"),
				row.list("procedure_number"), row.list("active_substances"), row.list("dhpc_type"),
				row.text("regulatory_outcome"), row.text("referral_name"), row.list("atc_code_human"),
				row.text("atcvet_code_veterinary"), row.list("therapeutic_area_mesh"), row.list("species"),
				row.list("other_related_medicines_nationally_authorised"), row.date("dissemination_date"),
				row.date("first_published_date"), row.date("last_updated_date"), row.text("dhpc_url"));
	}

	/**
	 * One page of communications, as the <code>EmaSafetyCommunicationPage</code> type.
	 */
	record Page(List<EmaSafetyCommunication> communications, int page, int totalPages, int totalCommunications) {

	}

}
