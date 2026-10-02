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

package org.springframework.samples.petclinic.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;

/**
 * Runs GraphQL queries against the sample data of the H2 database.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
class PetclinicGraphQlIntegrationTests {

	@Autowired
	private GraphQlTester graphQl;

	@Test
	void ownerWithPetsAndVisits() {
		this.graphQl.document("""
				{
				  owner(id: 6) {
				    firstName lastName city
				    pets { name birthDate type { name } visits { date description } }
				  }
				}""")
			.execute()
			.path("owner.lastName")
			.entity(String.class)
			.isEqualTo("Coleman")
			.path("owner.pets[*].name")
			.entityList(String.class)
			.containsExactly("Max", "Samantha")
			.path("owner.pets[0].type.name")
			.entity(String.class)
			.isEqualTo("cat")
			.path("owner.pets[0].visits[*].date")
			.entityList(String.class)
			.containsExactly("2013-01-02", "2013-01-03");
	}

	@Test
	void ownersByLastName() {
		this.graphQl.document("{ owners(lastName: \"Davis\") { totalOwners owners { firstName } } }")
			.execute()
			.path("owners.totalOwners")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("owners.owners[*].firstName")
			.entityList(String.class)
			.hasSize(2)
			.contains("Betty", "Harold");
	}

	@Test
	void vetsWithSpecialties() {
		this.graphQl.document("{ vets { lastName specialties { name } } }")
			.execute()
			.path("vets[?(@.lastName == 'Douglas')].specialties[*].name")
			.entityList(String.class)
			.containsExactly("dentistry", "surgery");
	}

	@Test
	void petTypesSortedByName() {
		this.graphQl.document("{ petTypes { name } }")
			.execute()
			.path("petTypes[*].name")
			.entityList(String.class)
			.containsExactly("bird", "cat", "dog", "hamster", "lizard", "snake");
	}

}
