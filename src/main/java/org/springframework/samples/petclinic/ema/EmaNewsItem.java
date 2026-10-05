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
 * A news item or press release of EMA. This is the <code>EmaNewsItem</code> type of the
 * GraphQL schema, read from EMA's report <code>news-json-report_en</code>. The schema
 * describes each field.
 */
record EmaNewsItem(String title, Boolean pressRelease, List<String> relatedMedicines, List<String> categories,
		List<String> topics, String summary, LocalDate firstPublishedDate, LocalDate lastUpdatedDate,
		String url) implements EmaPublished {

	static EmaNewsItem from(EmaRecord row) {
		return new EmaNewsItem(row.text("title"), row.yes("press_release"), row.list("related_medicine_referral"),
				row.list("categories"), row.list("topics"), row.text("news_summary"), row.date("first_published_date"),
				row.date("last_updated_date"), row.text("news_url"));
	}

	/**
	 * One page of news items, as the <code>EmaNewsItemPage</code> type.
	 */
	record Page(List<EmaNewsItem> news, int page, int totalPages, int totalNews) {

	}

}
