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
package org.springframework.samples.petclinic.upstream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.TestSocketUtils;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Runs the application with the <code>mock</code> profile and queries the external
 * services through GraphQL. The answers come from the recorded responses in
 * <code>src/main/resources/mock-upstreams</code>.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
@ActiveProfiles("mock")
class MockUpstreamIntegrationTests {

	@Autowired
	private GraphQlTester graphQl;

	@Autowired
	private WireMockServer mockUpstreams;

	@DynamicPropertySource
	static void mockPort(DynamicPropertyRegistry registry) {
		// the port is read twice, by the mock server and by the URLs of the clients, so
		// it is chosen once here
		int port = TestSocketUtils.findAvailableTcpPort();
		registry.add("petclinic.mock.port", () -> port);
	}

	@Test
	void dogZonesNearestToTheOwnerInAntwerp() {
		this.graphQl.document("""
				{
				  owners(lastName: "Oxx") {
				    owners { firstName nearestDogZones { name district distanceInMetres } }
				  }
				}""")
			.execute()
			.path("owners.owners[0].nearestDogZones[*].name")
			.entityList(String.class)
			.containsExactly("GROENENDAALLAAN", "COLUMBIASTRAAT (TUSSEN DE LANGBLOKKEN)", "HENDRIK VAN BOUTERSEMSTRAAT")
			.path("owners.owners[0].nearestDogZones[0].district")
			.entity(String.class)
			.isEqualTo("MERKSEM")
			.path("owners.owners[0].nearestDogZones[0].distanceInMetres")
			.entity(Integer.class)
			.isEqualTo(418);
	}

	@Test
	void ownersInWisconsinCostNoSoapCall() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ owners { owners { lastName nearestDogZones { name } } } }")
			.execute()
			.path("owners.owners[*].nearestDogZones[*]")
			.entityList(Object.class)
			.hasSize(0);

		this.mockUpstreams.verify(0, postRequestedFor(urlPathMatching("/antwerp/.*")));
	}

	@Test
	void veterinaryMedicinesForDogsFromEma() {
		this.graphQl.document("""
				{
				  emaMedicines(category: VETERINARY, species: "dogs", size: 3) {
				    totalMedicines
				    medicines { name category targetSpecies lastUpdatedDate }
				  }
				}""")
			.execute()
			.path("emaMedicines.totalMedicines")
			.entity(Integer.class)
			.isEqualTo(22)
			.path("emaMedicines.medicines[*].name")
			.entityList(String.class)
			.containsExactly("Comfortis", "Activyl Tick Plus", "Spironolactone Ceva")
			.path("emaMedicines.medicines[0].targetSpecies")
			.entityList(String.class)
			.containsExactly("Dogs", "Cats")
			.path("emaMedicines.medicines[0].lastUpdatedDate")
			.entity(String.class)
			.isEqualTo("2023-06-26");
	}

	@Test
	void petFoodAlertFromRasff() {
		this.graphQl.document("""
				{
				  rasffNotifications(filter: { productCategoryIds: ["18429"] }, size: 1) {
				    totalNotifications
				    notifications { id reference subject }
				  }
				  rasffNotification(id: "876211") {
				    validationDate
				    product { name category { description } hazards { name category } }
				    risk { decision }
				    countries { code roles }
				  }
				}""")
			.execute()
			.path("rasffNotifications.totalNotifications")
			.entity(Integer.class)
			.isEqualTo(270)
			.path("rasffNotifications.notifications[0].subject")
			.entity(String.class)
			.isEqualTo("Foreign body in pet food for cats from Germany")
			.path("rasffNotification.validationDate")
			.entity(String.class)
			.isEqualTo("2026-09-29")
			.path("rasffNotification.product.category.description")
			.entity(String.class)
			.isEqualTo("pet food")
			.path("rasffNotification.product.hazards[0].name")
			.entity(String.class)
			.isEqualTo("foreign body - foreign bodies")
			.path("rasffNotification.risk.decision")
			.entity(String.class)
			.isEqualTo("potential risk")
			.path("rasffNotification.countries[0].roles")
			.entityList(String.class)
			.containsExactly("FOR_FOLLOW_UP", "ORIGIN", "OPERATOR");
	}

	@Test
	void rasffAnswersAnUnknownNotificationWithUnauthorized() {
		this.graphQl.document("{ rasffNotification(id: \"1\") { reference } }")
			.execute()
			.path("rasffNotification")
			.valueIsNull();
	}

	@Test
	void dogSpeciesFromFamhpInEnglish() {
		this.graphQl.document("{ famhpTargetSpecies(name: \"dog\") { code name } }")
			.execute()
			.path("famhpTargetSpecies[0].code")
			.entity(String.class)
			.isEqualTo("Ca")
			.path("famhpTargetSpecies[0].name")
			.entity(String.class)
			.isEqualTo("Dog");
	}

	@Test
	void petPriceIndexOfBelgiumFromEurostat() {
		this.graphQl.document("""
				{
				  eurostatPetPriceIndex {
				    countryName unit
				    observations { period value }
				  }
				}""")
			.execute()
			.path("eurostatPetPriceIndex.countryName")
			.entity(String.class)
			.isEqualTo("Belgium")
			.path("eurostatPetPriceIndex.unit")
			.entity(String.class)
			.isEqualTo("Index, 2025=100")
			.path("eurostatPetPriceIndex.observations")
			.entityList(Object.class)
			.hasSize(12)
			.path("eurostatPetPriceIndex.observations[0].value")
			.entity(Double.class)
			.isEqualTo(99.68)
			// Eurostat lists the latest month before it publishes its value
			.path("eurostatPetPriceIndex.observations[11].period")
			.entity(String.class)
			.isEqualTo("2026-09")
			.path("eurostatPetPriceIndex.observations[11].value")
			.valueIsNull();
	}

	@Test
	void dogOccurrencesInBelgiumFromGbif() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  gbifSearchOccurrences(taxonKey: [6164210], country: [BE], page: 2, size: 2) {
				    count
				    results { key scientificName countryCode eventDate }
				  }
				}""")
			.execute()
			.path("gbifSearchOccurrences.count")
			.entity(Long.class)
			.isEqualTo(3749L)
			.path("gbifSearchOccurrences.results[0].scientificName")
			.entity(String.class)
			.isEqualTo("Canis familiaris Linnaeus, 1758")
			.path("gbifSearchOccurrences.results[0].countryCode")
			.entity(String.class)
			.isEqualTo("BE");

		// the page and the size of the schema become the offset and the limit of GBIF
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/gbif/v1/occurrence/search"))
			.withQueryParam("taxonKey", equalTo("6164210"))
			.withQueryParam("country", equalTo("BE"))
			.withQueryParam("offset", equalTo("2"))
			.withQueryParam("limit", equalTo("2")));
	}

	@Test
	void dogInTheGbifBackboneWithCountsByKingdom() {
		this.graphQl.document("""
				{
				  gbifNameUsage(usageKey: 6164210) { scientificName rank family }
				  gbifDatasetMetrics(key: "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c") { countByKingdom { key value } }
				}""")
			.execute()
			.path("gbifNameUsage.scientificName")
			.entity(String.class)
			.isEqualTo("Canis lupus familiaris Linnaeus, 1758")
			.path("gbifNameUsage.rank")
			.entity(String.class)
			.isEqualTo("SUBSPECIES")
			// GBIF answers with a map of counts, and the schema has a list of entries
			.path("gbifDatasetMetrics.countByKingdom[0].key")
			.entity(String.class)
			.isEqualTo("ANIMALIA")
			.path("gbifDatasetMetrics.countByKingdom[0].value")
			.entity(Integer.class)
			.isEqualTo(2909803);
	}

	@Test
	void gbifAnswerWithoutContentBecomesNull() {
		this.graphQl.document("""
				{ gbifInstitutionMasterSourceMetadata(key: "6f7571ff-9943-4dee-9707-48743806493e") { source } }""")
			.execute()
			.path("gbifInstitutionMasterSourceMetadata")
			.valueIsNull();
	}

}
