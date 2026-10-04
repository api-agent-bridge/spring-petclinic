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
package org.springframework.samples.petclinic.dogzone;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An off-leash dog zone of the City of Antwerp, as the <code>DogZone</code> type of the
 * GraphQL schema. The city publishes the data in Dutch, and {@link #fromRecord(Map, int)}
 * translates it.
 *
 * @param id the city's identifier of the zone
 * @param name the name of the zone
 * @param street the street the zone is on
 * @param postalCode the postal code
 * @param district the district of Antwerp, or the neighbouring municipality
 * @param areaInSquareMetres the size of the zone
 * @param cleanliness how clean inspectors found the zone
 * @param lighting the kind of lighting
 * @param fenceHeightsInMetres the heights of the fences
 * @param fenceMaterial what the fence is made of
 * @param bins the number of bins
 * @param benches the number of benches
 * @param surface the ground surface
 * @param distanceInMetres the distance from the address the zones were searched around
 */
record DogZone(String id, String name, String street, String postalCode, String district, Integer areaInSquareMetres,
		Rating cleanliness, Lighting lighting, List<Double> fenceHeightsInMetres, String fenceMaterial, Integer bins,
		Integer benches, String surface, int distanceInMetres) {

	/**
	 * How clean a zone is. The source uses free text, so only the values with one clear
	 * meaning are mapped.
	 */
	enum Rating {

		GOOD, ACCEPTABLE, BAD

	}

	enum Lighting {

		DIRECT, INDIRECT, NONE

	}

	// "aanvaarbaar" is a misspelling of "aanvaardbaar" that occurs in the source data
	private static final Map<String, Rating> RATINGS = Map.of("goed", Rating.GOOD, "aanvaardbaar", Rating.ACCEPTABLE,
			"aanvaarbaar", Rating.ACCEPTABLE, "slecht", Rating.BAD);

	private static final Map<String, Lighting> LIGHTING = Map.of("rechtstreeks", Lighting.DIRECT, "onrechtstreeks",
			Lighting.INDIRECT, "geen", Lighting.NONE);

	private static final Map<String, String> SURFACES = Map.of("gras", "grass", "zand", "sand", "onkruid", "weeds",
			"gras/zand", "grass and sand");

	private static final Map<String, String> FENCE_MATERIALS = Map.of("staalmat", "steel mesh panels",
			"staalmat/maasdraad", "steel mesh panels and wire mesh", "maasdraad met bovenbuis",
			"wire mesh with a top rail", "maasdraad zonder bovenbuis", "wire mesh without a top rail",
			"maasdraad in ligustrum haag", "wire mesh inside a privet hedge");

	// heights are typed by hand: "1m05", "1m 05", "0lm95", "2m00 en 1m00"
	private static final Pattern HEIGHT = Pattern.compile("(\\d)\\s*l?m\\s*(\\d{2})");

	/**
	 * Translates one record of the city's layer, keyed by its Dutch field names.
	 */
	static DogZone fromRecord(Map<String, String> record, int distanceInMetres) {
		return new DogZone(record.get("id"), record.get("naam"), record.get("straatnaam"), record.get("postcode"),
				record.get("district"), squareMetres(record.get("opp_ha")),
				RATINGS.get(normalise(record.get("netheid"))), LIGHTING.get(normalise(record.get("verlichting"))),
				heights(record.get("hoogte_afsluiting")),
				translate(FENCE_MATERIALS, record.get("materiaal_afsluiting")),
				integer(record.get("aantal_vuilnisbakken")), integer(record.get("aantal_zitbanken")),
				translate(SURFACES, record.get("type_ondergrond")), distanceInMetres);
	}

	private static String normalise(String value) {
		// the source writes the same value as "gras/zand" and "gras/ zand"
		return (value != null)
				? value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s*/\\s*", "/").replaceAll("\\s+", " ") : "";
	}

	/**
	 * Returns the English text for a known Dutch value, and the original text for a value
	 * this class has not seen before.
	 */
	private static String translate(Map<String, String> glossary, String value) {
		return (value != null) ? glossary.getOrDefault(normalise(value), value) : null;
	}

	private static Integer squareMetres(String hectares) {
		return (hectares != null) ? (int) Math.round(Double.parseDouble(hectares) * 10_000) : null;
	}

	private static Integer integer(String value) {
		return (value != null) ? Integer.valueOf(value) : null;
	}

	private static List<Double> heights(String value) {
		if (value == null) {
			return List.of();
		}
		Matcher matcher = HEIGHT.matcher(value);
		return matcher.results().map((height) -> Double.parseDouble(height.group(1) + "." + height.group(2))).toList();
	}

}
