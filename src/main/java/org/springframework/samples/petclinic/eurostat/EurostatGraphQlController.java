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

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Answers the <code>eurostat*</code> queries of the GraphQL schema from the harmonised
 * index of consumer prices (HICP). Each query reads one series of one dataset: the prices
 * of one pet category in one country. Every query costs one call to Eurostat.
 * <p>
 * The datasets follow the 2026 version of the European classification of consumption
 * (ECOICOP 2), which numbers the pet categories <code>CP0932</code> and
 * <code>CP0945</code>.
 */
@Controller
class EurostatGraphQlController {

	static final String EUROSTAT = "Eurostat";

	private static final String MONTHLY = "prc_hicp_minr";

	private static final String ANNUAL = "prc_hicp_ainr";

	private static final String ITEM_WEIGHTS = "prc_hicp_iw";

	private static final String CONSTANT_TAX_RATES = "prc_hicp_ct";

	private static final String EURO_AREA_CONTRIBUTIONS = "prc_hicp_ctr";

	private static final String EURO_AREA = "EA";

	// two letters for a country, more for a group such as EA or EU27_2020
	private static final Pattern COUNTRY = Pattern.compile("[A-Z][A-Z0-9_]{1,9}");

	private static final Pattern MONTH = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

	private static final Pattern YEAR = Pattern.compile("\\d{4}");

	/**
	 * The pet categories of the price index, as the ECOICOP 2 codes Eurostat uses.
	 */
	enum PetCategory {

		PETS_AND_PET_PRODUCTS("CP0932"), PETS("CP09321"), PET_PRODUCTS("CP09322"), VETERINARY_SERVICES("CP0945");

		private final String code;

		PetCategory(String code) {
			this.code = code;
		}

	}

	private final EurostatApi eurostat;

	EurostatGraphQlController(EurostatApi eurostat) {
		this.eurostat = eurostat;
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceIndex(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return monthly(MONTHLY, "I25", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceMonthlyChange(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return monthly(MONTHLY, "RCH_M", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceAnnualChange(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return monthly(MONTHLY, "RCH_A", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceMovingAverageChange(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return monthly(MONTHLY, "RCH_MV12MAVR", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceIndexAtConstantTaxRates(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return monthly(CONSTANT_TAX_RATES, "I25", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceContributionToEuroAreaInflation(@Argument PetCategory category,
			@Argument String from, @Argument String to) {
		return monthly(EURO_AREA_CONTRIBUTIONS, "PC_PNT", category, EURO_AREA, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceAnnualAverageIndex(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return annual(ANNUAL, "unit", "INX_A_AVG", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetPriceAnnualAverageChange(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return annual(ANNUAL, "unit", "RCH_A_AVG", category, country, from, to);
	}

	@QueryMapping
	EurostatSeries eurostatPetSpendingWeight(@Argument PetCategory category, @Argument String country,
			@Argument String from, @Argument String to) {
		return annual(ITEM_WEIGHTS, "statinfo", "IW", category, country, from, to);
	}

	private EurostatSeries monthly(String dataset, String unit, PetCategory category, String country, String from,
			String to) {
		MultiValueMap<String, String> filters = filters(category, country, from, to, MONTH, "YYYY-MM", 12);
		filters.add("unit", unit);
		return series(dataset, country, filters, null);
	}

	private EurostatSeries annual(String dataset, String unitDimension, String unit, PetCategory category,
			String country, String from, String to) {
		MultiValueMap<String, String> filters = filters(category, country, from, to, YEAR, "YYYY", 5);
		filters.add(unitDimension, unit);
		// the item weights lack a unit dimension; Eurostat gives them in parts per
		// thousand of household spending
		return series(dataset, country, filters, ITEM_WEIGHTS.equals(dataset) ? "Per mille" : null);
	}

	private EurostatSeries series(String dataset, String country, MultiValueMap<String, String> filters, String unit) {
		String countryCode = country.strip().toUpperCase(Locale.ROOT);
		return EurostatSeries.from(dataset, countryCode, unit,
				RestUpstreams.call(EUROSTAT, () -> this.eurostat.data(dataset, filters)));
	}

	/**
	 * Builds the filters of a query. Without a first and a last period, the series covers
	 * the latest periods Eurostat has published.
	 */
	private static MultiValueMap<String, String> filters(PetCategory category, String country, String from, String to,
			Pattern period, String periodFormat, int latestPeriods) {
		String countryCode = country.strip().toUpperCase(Locale.ROOT);
		if (!COUNTRY.matcher(countryCode).matches()) {
			throw new InvalidArgumentException(
					"country must be a Eurostat country code such as BE, or a group such as EU27_2020");
		}
		MultiValueMap<String, String> filters = new LinkedMultiValueMap<>();
		filters.add("coicop18", category.code);
		filters.add("geo", countryCode);
		if (from == null && to == null) {
			filters.add("lastTimePeriod", String.valueOf(latestPeriods));
		}
		if (from != null) {
			filters.add("sinceTimePeriod", period(from, "from", period, periodFormat));
		}
		if (to != null) {
			filters.add("untilTimePeriod", period(to, "to", period, periodFormat));
		}
		return filters;
	}

	private static String period(String value, String argument, Pattern period, String periodFormat) {
		String stripped = value.strip();
		if (!period.matcher(stripped).matches()) {
			throw new InvalidArgumentException(argument + " must be a period written " + periodFormat);
		}
		return stripped;
	}

}
