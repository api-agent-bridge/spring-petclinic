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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * One record of an EMA report, read as the types of the GraphQL schema need it. Every
 * value of a report is a text, so this class converts them:
 * <ul>
 * <li>An empty text is a missing value, and becomes null.</li>
 * <li>A date is written 04/10/2026 in most reports and as an ISO-8601 timestamp in the
 * document reports.</li>
 * <li>A yes-or-no column holds the texts Yes and No.</li>
 * <li>A column with several values separates them with semicolons, and sometimes repeats
 * a value.</li>
 * </ul>
 */
final class EmaRecord {

	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final Map<String, Object> values;

	EmaRecord(Map<String, Object> values) {
		this.values = values;
	}

	/**
	 * The text of a column, or null when it is empty or missing.
	 */
	String text(String column) {
		if (!(this.values.get(column) instanceof String text) || text.isBlank()) {
			return null;
		}
		return text.strip();
	}

	/**
	 * The values of a column that holds several, each once, in the order of the report.
	 */
	List<String> list(String column) {
		String text = text(column);
		if (text == null) {
			return List.of();
		}
		return Arrays.stream(text.split(";"))
			.map(String::strip)
			.filter((value) -> !value.isEmpty())
			.distinct()
			.toList();
	}

	/**
	 * The day of a date column, or null when it is empty or cannot be read.
	 */
	LocalDate date(String column) {
		String text = text(column);
		if (text == null) {
			return null;
		}
		try {
			return text.contains("/") ? LocalDate.parse(text, DAY_MONTH_YEAR) : LocalDate.parse(text.substring(0, 10));
		}
		catch (DateTimeParseException | StringIndexOutOfBoundsException ex) {
			return null;
		}
	}

	/**
	 * True for Yes, false for No, and null for anything else.
	 */
	Boolean yes(String column) {
		String text = text(column);
		if ("Yes".equalsIgnoreCase(text)) {
			return true;
		}
		return "No".equalsIgnoreCase(text) ? false : null;
	}

	Integer integer(String column) {
		String text = text(column);
		try {
			return (text != null) ? Integer.valueOf(text) : null;
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	/**
	 * The category of the record, read from the texts Human and Veterinary.
	 */
	EmaCategory category(String column) {
		String text = text(column);
		if ("Human".equalsIgnoreCase(text)) {
			return EmaCategory.HUMAN;
		}
		return "Veterinary".equalsIgnoreCase(text) ? EmaCategory.VETERINARY : null;
	}

}
