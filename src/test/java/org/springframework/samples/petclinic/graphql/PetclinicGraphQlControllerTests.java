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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static graphql.ErrorType.ValidationError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.graphql.execution.ErrorType.BAD_REQUEST;

/**
 * Test class for the {@link PetclinicGraphQlController}
 */
@GraphQlTest(PetclinicGraphQlController.class)
@DisabledInNativeImage
@DisabledInAotMode
class PetclinicGraphQlControllerTests {

	@Autowired
	private GraphQlTester graphQl;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private PetTypeRepository petTypes;

	private static PageRequest page(int index, int size) {
		return PageRequest.of(index, size, PetclinicGraphQlController.OWNER_ORDER);
	}

	private Owner george() {
		Owner george = new Owner();
		george.setId(1);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");
		PetType cat = new PetType();
		cat.setId(1);
		cat.setName("cat");
		Pet leo = new Pet();
		leo.setId(1);
		leo.setName("Leo");
		leo.setType(cat);
		leo.setBirthDate(LocalDate.of(2010, 9, 7));
		Visit visit = new Visit();
		visit.setId(1);
		visit.setDate(LocalDate.of(2013, 1, 4));
		visit.setDescription("rabies shot");
		leo.addVisit(visit);
		george.addPet(leo);
		return george;
	}

	@Test
	void ownerReturnsPetsAndVisits() {
		given(this.owners.findById(1)).willReturn(Optional.of(george()));

		this.graphQl.document("""
				{
				  owner(id: 1) {
				    id firstName lastName address city telephone
				    pets { name birthDate type { name } visits { date description } }
				  }
				}""")
			.execute()
			.path("owner.lastName")
			.entity(String.class)
			.isEqualTo("Franklin")
			.path("owner.pets[0].name")
			.entity(String.class)
			.isEqualTo("Leo")
			.path("owner.pets[0].birthDate")
			.entity(String.class)
			.isEqualTo("2010-09-07")
			.path("owner.pets[0].type.name")
			.entity(String.class)
			.isEqualTo("cat")
			.path("owner.pets[0].visits[0].date")
			.entity(String.class)
			.isEqualTo("2013-01-04")
			.path("owner.pets[0].visits[0].description")
			.entity(String.class)
			.isEqualTo("rabies shot");
	}

	@Test
	void dateScalarTakesItsDescriptionAndSpecificationFromTheSchemaFile() {
		this.graphQl.document("{ __type(name: \"Date\") { description specifiedByURL } }")
			.execute()
			.path("__type.description")
			.entity(String.class)
			.satisfies((description) -> assertThat(description).contains("YYYY-MM-DD"))
			.path("__type.specifiedByURL")
			.entity(String.class)
			.isEqualTo("https://scalars.graphql.org/andimarek/local-date.html");
	}

	@Test
	void ownerIsNullWhenTheIdIsUnknown() {
		given(this.owners.findById(99)).willReturn(Optional.empty());

		this.graphQl.document("{ owner(id: 99) { lastName } }").execute().path("owner").valueIsNull();
	}

	@Test
	void ownerIsNullWhenTheIdIsNotAnInteger() {
		this.graphQl.document("{ owner(id: \"abc\") { lastName } }").execute().path("owner").valueIsNull();
		this.graphQl.document("{ owner(id: \"99999999999\") { lastName } }").execute().path("owner").valueIsNull();
	}

	@Test
	void ownersUsesTheSamePagingAsTheWebPages() {
		given(this.owners.findByLastNameStartingWith("", page(0, 5)))
			.willReturn(new PageImpl<>(List.of(george()), page(0, 5), 11));

		this.graphQl.document("{ owners { page totalPages totalOwners owners { lastName } } }")
			.execute()
			.path("owners.page")
			.entity(Integer.class)
			.isEqualTo(1)
			.path("owners.totalPages")
			.entity(Integer.class)
			.isEqualTo(3)
			.path("owners.totalOwners")
			.entity(Integer.class)
			.isEqualTo(11)
			.path("owners.owners[*].lastName")
			.entityList(String.class)
			.containsExactly("Franklin");
	}

	@Test
	void ownersFiltersByLastNameAndPage() {
		given(this.owners.findByLastNameStartingWith("Dav", page(1, 2)))
			.willReturn(new PageImpl<>(List.of(), page(1, 2), 2));

		this.graphQl.document("{ owners(lastName: \" Dav \", page: 2, size: 2) { page totalPages } }")
			.execute()
			.path("owners.page")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("owners.totalPages")
			.entity(Integer.class)
			.isEqualTo(1);
	}

	@Test
	void ownersClampsPageAndSizeFromTheClient() {
		given(this.owners.findByLastNameStartingWith("", page(0, 50)))
			.willReturn(new PageImpl<>(List.of(), page(0, 50), 0));

		this.graphQl.document("{ owners(page: -2147483648, size: 2147483647) { page } }")
			.execute()
			.path("owners.page")
			.entity(Integer.class)
			.isEqualTo(1);

		verify(this.owners).findByLastNameStartingWith("", page(0, 50));
	}

	@Test
	void ownersRejectsANullPageOrSize() {
		for (String arguments : List.of("page: null", "size: null")) {
			this.graphQl.document("{ owners(" + arguments + ") { page } }")
				.execute()
				.errors()
				.satisfy((errors) -> assertThat(errors).singleElement()
					.satisfies((error) -> assertThat(error.getErrorType()).isEqualTo(ValidationError)));
		}

		verifyNoInteractions(this.owners);
	}

	@Test
	void ownersReportsANullPageFromAVariableAsABadRequest() {
		// validation accepts a nullable variable here because the argument has a default,
		// so the null is found when the query runs
		this.graphQl.document("query($page: Int) { owners(page: $page) { page } }")
			.variable("page", null)
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(BAD_REQUEST);
				assertThat(error.getMessage()).contains("'page'");
			}));

		verifyNoInteractions(this.owners);
	}

	@Test
	void vetsReturnsSpecialtiesSortedByName() {
		Specialty surgery = new Specialty();
		surgery.setId(2);
		surgery.setName("surgery");
		Specialty dentistry = new Specialty();
		dentistry.setId(3);
		dentistry.setName("dentistry");
		Vet linda = new Vet();
		linda.setId(3);
		linda.setFirstName("Linda");
		linda.setLastName("Douglas");
		linda.addSpecialty(surgery);
		linda.addSpecialty(dentistry);
		given(this.vets.findAll()).willReturn(List.of(linda));

		this.graphQl.document("{ vets { firstName lastName specialties { name } } }")
			.execute()
			.path("vets[0].lastName")
			.entity(String.class)
			.isEqualTo("Douglas")
			.path("vets[0].specialties[*].name")
			.entityList(String.class)
			.containsExactly("dentistry", "surgery");
	}

	@Test
	void petTypesReturnsTheTypesFromTheRepository() {
		PetType cat = new PetType();
		cat.setId(1);
		cat.setName("cat");
		PetType dog = new PetType();
		dog.setId(2);
		dog.setName("dog");
		given(this.petTypes.findPetTypes()).willReturn(List.of(cat, dog));

		this.graphQl.document("{ petTypes { id name } }")
			.execute()
			.path("petTypes[*].name")
			.entityList(String.class)
			.containsExactly("cat", "dog")
			.path("petTypes[0].id")
			.entity(String.class)
			.satisfies((id) -> assertThat(id).isEqualTo("1"));
	}

}
