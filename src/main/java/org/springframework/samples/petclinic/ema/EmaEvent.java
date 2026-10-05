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
 * An event that EMA organises or takes part in, such as a workshop or a committee
 * meeting. This is the <code>EmaEvent</code> type of the GraphQL schema, read from EMA's
 * report <code>events-json-report_en</code>. The schema describes each field.
 */
record EmaEvent(String title, String dates, Boolean liveBroadcast, Boolean online, String location,
		LocalDate firstPublishedDate, LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaEvent from(EmaRecord row) {
		return new EmaEvent(row.text("title"), row.text("date_start_end_dates"), row.yes("live_broadcast"),
				row.yes("online"), row.text("location"), row.date("first_published_date"),
				row.date("last_updated_date"), row.text("event_url"));
	}

	/**
	 * One page of events, as the <code>EmaEventPage</code> type.
	 */
	record Page(List<EmaEvent> events, int page, int totalPages, int totalEvents) {

	}

}
