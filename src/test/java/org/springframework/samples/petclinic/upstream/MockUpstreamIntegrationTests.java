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

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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

}
