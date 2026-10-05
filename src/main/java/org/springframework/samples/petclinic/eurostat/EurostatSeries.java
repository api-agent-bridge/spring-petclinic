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
package org.springframework.samples.petclinic.eurostat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.samples.petclinic.eurostat.EurostatApi.Dataset;
import org.springframework.samples.petclinic.eurostat.EurostatApi.Dimension;
import org.springframework.samples.petclinic.upstream.UpstreamException;

/**
 * A time series of one Eurostat dataset, as the <code>EurostatSeries</code> type of the
 * GraphQL schema: one price category in one country, with one observation per period.
 *
 * @param dataset the code of the dataset
 * @param title the title of the dataset
 * @param category the name of the price category
 * @param country the code of the country or group of countries
 * @param countryName the name of the country, null when Eurostat does not know the code
 * @param unit what the values measure
 * @param lastUpdate the day Eurostat last updated the dataset
 * @param observations the observations, oldest first
 */
record EurostatSeries(String dataset, String title, String category, String country, String countryName, String unit,
		LocalDate lastUpdate, List<Observation> observations) {

	/**
	 * One observation of a series.
	 *
	 * @param period the month as YYYY-MM, or the year as YYYY
	 * @param value the value, null when Eurostat has not published it
	 * @param flag Eurostat's flag, for example <code>e</code> for an estimate or
	 * <code>p</code> for a provisional value
	 */
	record Observation(String period, Double value, String flag) {

	}

	/**
	 * Reads the series from a dataset filtered to one value of every dimension except
	 * time. The values are then numbered by their position in time.
	 * @param unit what the values measure, used when the dataset lacks a unit dimension
	 */
	static EurostatSeries from(String datasetCode, String country, String unit, Dataset dataset) {
		if (dataset.size().contains(0)) {
			// a filter value the dataset does not know leaves that dimension empty
			return new EurostatSeries(datasetCode, dataset.label(), label(dataset, "coicop18"), country, null,
					unit(dataset, unit), lastUpdate(dataset), List.of());
		}
		int series = dataset.size().stream().reduce(1, Math::multiplyExact)
				/ dataset.size().get(dataset.id().indexOf("time"));
		if (series != 1) {
			throw new UpstreamException(EurostatGraphQlController.EUROSTAT,
					"the answer for " + datasetCode + " holds " + series + " series");
		}
		Map<String, Integer> periods = dataset.dimension().get("time").category().index();
		List<Observation> observations = periods.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
			.map((period) -> {
				String number = String.valueOf(period.getValue());
				return new Observation(period.getKey(), dataset.value().get(number),
						(dataset.status() != null) ? dataset.status().get(number) : null);
			})
			.toList();
		return new EurostatSeries(datasetCode, dataset.label(), label(dataset, "coicop18"), country,
				label(dataset, "geo"), unit(dataset, unit), lastUpdate(dataset), observations);
	}

	private static String unit(Dataset dataset, String unit) {
		String label = label(dataset, "unit");
		return (label != null) ? label : unit;
	}

	/**
	 * Returns the name of the one category of a dimension.
	 */
	private static String label(Dataset dataset, String dimensionName) {
		Dimension dimension = dataset.dimension().get(dimensionName);
		if (dimension == null || dimension.category().label() == null) {
			return null;
		}
		return dimension.category().label().values().stream().findFirst().orElse(null);
	}

	private static LocalDate lastUpdate(Dataset dataset) {
		// for example 2026-10-02T11:00:00+0200
		return (dataset.updated() != null) ? LocalDate.parse(dataset.updated().substring(0, 10)) : null;
	}

}
