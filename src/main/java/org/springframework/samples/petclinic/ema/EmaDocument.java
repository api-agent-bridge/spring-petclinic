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
 * A document published on the EMA website, such as an assessment report, a product
 * information leaflet or a press release. This is the <code>EmaDocument</code> type of
 * the GraphQL schema, read from EMA's reports
 * <code>documents-output-epar_documents_json-report_en</code> and
 * <code>documents-output-non_epar_documents_json-report_en</code>. The schema describes
 * each field.
 */
record EmaDocument(String id, String name, String type, String medicineName, String productNumber, String status,
		String consultationPeriod, String referenceNumber, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaDocument from(EmaRecord row) {
		return new EmaDocument(row.text("id"), row.text("name"), row.text("type"), row.text("medicine_name"),
				row.text("ema_product_number"), row.text("status"), row.text("consultation_date"),
				row.text("reference_number"), row.date("first_published_date"), row.date("last_updated_date"),
				row.text("document_url"));
	}

	/**
	 * One page of documents, as the <code>EmaDocumentPage</code> type.
	 */
	record Page(List<EmaDocument> documents, int page, int totalPages, int totalDocuments) {

	}

}
