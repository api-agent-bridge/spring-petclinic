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
import java.util.Comparator;

/**
 * A record of an EMA report. Every report has the day a record was first published and
 * the day it was last updated, and the schema lists the records most recently changed
 * first.
 */
interface EmaPublished {

	/**
	 * Orders records by the day of their latest change, the most recent first. Records
	 * changed on the same day keep the order of the report.
	 */
	Comparator<EmaPublished> LATEST_FIRST = Comparator.comparing(EmaPublished::latestChange,
			Comparator.nullsLast(Comparator.reverseOrder()));

	LocalDate firstPublishedDate();

	LocalDate lastUpdatedDate();

	/**
	 * The day of the latest change: the last update, or the first publication for a
	 * record that has not been updated.
	 */
	default LocalDate latestChange() {
		return (lastUpdatedDate() != null) ? lastUpdatedDate() : firstPublishedDate();
	}

}
