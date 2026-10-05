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
 * A general page of the EMA website, such as guidance on a procedure or an overview of a
 * topic. This is the <code>EmaWebsiteArticle</code> type of the GraphQL schema, read from
 * EMA's report <code>general-json-report_en</code>. The schema describes each field.
 */
record EmaWebsiteArticle(String title, String summary, List<String> categories, LocalDate firstPublishedDate,
		LocalDate lastUpdatedDate, String url) implements EmaPublished {

	static EmaWebsiteArticle from(EmaRecord row) {
		return new EmaWebsiteArticle(row.text("title"), row.text("summary"), row.list("categories"),
				row.date("first_published_date"), row.date("last_updated_date"), row.text("general_url"));
	}

	/**
	 * One page of articles, as the <code>EmaWebsiteArticlePage</code> type.
	 */
	record Page(List<EmaWebsiteArticle> articles, int page, int totalPages, int totalArticles) {

	}

}
