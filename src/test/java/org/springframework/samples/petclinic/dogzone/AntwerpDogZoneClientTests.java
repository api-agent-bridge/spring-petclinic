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

import java.time.Duration;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.samples.petclinic.dogzone.DogZone.Lighting;
import org.springframework.samples.petclinic.dogzone.DogZone.Rating;
import org.springframework.samples.petclinic.upstream.UpstreamException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingXPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests the {@link AntwerpDogZoneClient} against the responses recorded from the City of
 * Antwerp, served by the same mock server the <code>mock</code> profile uses.
 */
class AntwerpDogZoneClientTests {

	private static final String MAP = "/antwerp/arcgissql/services/P_Portal/portal_publiek1/MapServer";

	private static final String GEOCODER = "/antwerp/arcgissql/services/LOC_CRAB/GeocodeServer";

	private static WireMockServer antwerp;

	private AntwerpDogZoneClient client;

	@BeforeAll
	static void startServer() {
		antwerp = new WireMockServer(options().dynamicPort().usingFilesUnderClasspath("mock-upstreams"));
		antwerp.start();
	}

	@AfterAll
	static void stopServer() {
		antwerp.stop();
	}

	@BeforeEach
	void setup() {
		antwerp.resetAll();
		this.client = client("portal_publiek1", Duration.ofSeconds(5));
	}

	private AntwerpDogZoneClient client(String mapName, Duration readTimeout) {
		return new AntwerpDogZoneClient(new WebServiceTemplateBuilder(), antwerp.baseUrl() + MAP, mapName, 9,
				antwerp.baseUrl() + GEOCODER, 2000, Duration.ofSeconds(2), readTimeout);
	}

	@Test
	void findsTheZonesNearestToAnAntwerpAddress() {
		List<DogZone> zones = this.client.findNearest("Groenendaallaan 394", "Antwerpen", 3);

		assertThat(zones).extracting(DogZone::name)
			.containsExactly("GROENENDAALLAAN", "COLUMBIASTRAAT (TUSSEN DE LANGBLOKKEN)",
					"HENDRIK VAN BOUTERSEMSTRAAT");
		// the city's REST interface gives 416, 966 and 999 m. The points of the SOAP
		// answer, read as plain Web Mercator, would give 354, 1001 and 956 m
		assertThat(zones).extracting(DogZone::distanceInMetres).containsExactly(418, 967, 1001);
	}

	@Test
	void translatesTheZonesIntoEnglish() {
		DogZone columbiastraat = this.client.findNearest("Groenendaallaan 394", "Antwerpen", 2).get(1);

		assertThat(columbiastraat.street()).isEqualTo("COLUMBIASTRAAT");
		assertThat(columbiastraat.postalCode()).isEqualTo("2030");
		assertThat(columbiastraat.district()).isEqualTo("ANTWERPEN");
		assertThat(columbiastraat.cleanliness()).isEqualTo(Rating.GOOD);
		assertThat(columbiastraat.lighting()).isEqualTo(Lighting.INDIRECT);
		assertThat(columbiastraat.benches()).isEqualTo(1);
	}

	@Test
	void keepsTheZonesOfTheSquareThatLieWithinTheRadius() {
		List<DogZone> zones = this.client.findNearest("Groenendaallaan 394", "Antwerpen", 100);

		// the map answers with the 17 zones of the square, and 15 lie within 2000 m
		assertThat(zones).hasSize(15);
		assertThat(zones).extracting(DogZone::distanceInMetres).isSorted().allMatch((distance) -> distance <= 2000);
	}

	@Test
	void sendsTheAddressAsPropertiesAndTheSquareInLambert72() {
		this.client.findNearest(" Groenendaallaan 394 ", " Antwerpen ", 3);

		antwerp.verify(1,
				postRequestedFor(urlPathEqualTo(GEOCODER))
					.withRequestBody(matchingXPath("//*[local-name()='GeocodeAddress']/Address/PropertyArray"
							+ "[PropertySetProperty[Key='Street' and Value='Groenendaallaan']]"
							+ "[PropertySetProperty[Key='House' and Value='394']]"
							+ "[PropertySetProperty[Key='City' and Value='Antwerpen']]")));
		antwerp.verify(1, postRequestedFor(urlPathEqualTo(MAP)).withRequestBody(
				matchingXPath("//*[local-name()='QueryFeatureData'][MapName='portal_publiek1'][LayerID='9']"
						+ "/QueryFilter[SpatialRel='esriSpatialRelIntersects']"
						+ "/FilterGeometry[SpatialReference/WKID='31370'][round(XMax - XMin) = 4000][round(YMax - YMin) = 4000]")));
	}

	@Test
	void returnsNoZonesForAnAddressTheGeocoderCannotFind() {
		assertThat(this.client.findNearest("Onbestaandestraat 1", "Antwerpen", 3)).isEmpty();

		antwerp.verify(1, postRequestedFor(urlPathEqualTo(GEOCODER)));
		antwerp.verify(0, postRequestedFor(urlPathEqualTo(MAP)));
	}

	@Test
	void leavesAnAddressThatStartsWithTheHouseNumberAlone() {
		assertThat(this.client.findNearest("110 W. Liberty St.", "Madison", 3)).isEmpty();

		antwerp.verify(0, anyRequestedFor(anyUrl()));
	}

	@Test
	void reportsASoapFaultThatArrivesWithStatus200() {
		AntwerpDogZoneClient wrongMap = client("wrong_map", Duration.ofSeconds(5));

		assertThatExceptionOfType(UpstreamException.class)
			.isThrownBy(() -> wrongMap.findNearest("Groenendaallaan 394", "Antwerpen", 3))
			.withMessageContaining("Error processing server request")
			.satisfies((ex) -> assertThat(ex.getUpstream()).isEqualTo(AntwerpDogZoneClient.MAP));
	}

	@Test
	void reportsAGeocoderThatAnswersTooSlowly() {
		antwerp.stubFor(post(urlPathEqualTo(GEOCODER)).atPriority(1)
			.willReturn(aResponse().withFixedDelay(1500).withBodyFile("antwerp/geocode-unmatched.xml")));
		AntwerpDogZoneClient impatient = client("portal_publiek1", Duration.ofMillis(300));

		assertThatExceptionOfType(UpstreamException.class)
			.isThrownBy(() -> impatient.findNearest("Groenendaallaan 394", "Antwerpen", 3))
			.satisfies((ex) -> assertThat(ex.getUpstream()).isEqualTo(AntwerpDogZoneClient.GEOCODER));
	}

}
