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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.samples.petclinic.dogzone.DogZone.Lighting;
import org.springframework.samples.petclinic.dogzone.DogZone.Rating;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.PetRepository;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test class for the {@link DogZoneGraphQlController}
 */
@GraphQlTest
@DisabledInNativeImage
@DisabledInAotMode
class DogZoneGraphQlControllerTests {

	@Autowired
	private GraphQlTester graphQl;

	@MockitoBean
	private AntwerpDogZoneClient dogZones;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetRepository pets;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private PetTypeRepository petTypes;

	private static DogZone zone(String name, int distanceInMetres) {
		return new DogZone("OPR0001", name, name, "2030", "ANTWERPEN", 2435, Rating.GOOD, Lighting.INDIRECT,
				List.of(1.0), "steel mesh panels", 1, 1, "grass", distanceInMetres);
	}

	private void devOxx() {
		Owner owner = new Owner();
		owner.setId(11);
		owner.setFirstName("Dev");
		owner.setLastName("Oxx");
		owner.setAddress("Groenendaallaan 394");
		owner.setCity("Antwerpen");
		owner.setTelephone("0123456789");
		given(this.owners.findById(11)).willReturn(Optional.of(owner));
	}

	@Test
	void nearestDogZonesOfAnOwner() {
		devOxx();
		given(this.dogZones.findNearest("Groenendaallaan 394", "Antwerpen", 3))
			.willReturn(List.of(zone("GROENENDAALLAAN", 418), zone("COLUMBIASTRAAT", 967)));

		this.graphQl.document("{ owner(id: 11) { nearestDogZones { name distanceInMetres cleanliness lighting } } }")
			.execute()
			.path("owner.nearestDogZones[*].name")
			.entityList(String.class)
			.containsExactly("GROENENDAALLAAN", "COLUMBIASTRAAT")
			.path("owner.nearestDogZones[0].distanceInMetres")
			.entity(Integer.class)
			.isEqualTo(418)
			.path("owner.nearestDogZones[0].cleanliness")
			.entity(String.class)
			.isEqualTo("GOOD")
			.path("owner.nearestDogZones[0].lighting")
			.entity(String.class)
			.isEqualTo("INDIRECT");
	}

	@Test
	void passesFirstToTheClient() {
		devOxx();

		this.graphQl.document("{ owner(id: 11) { nearestDogZones(first: 1) { name } } }").execute();

		verify(this.dogZones).findNearest("Groenendaallaan 394", "Antwerpen", 1);
	}

	@Test
	void rejectsAFirstOutsideOneToTen() {
		devOxx();

		this.graphQl.document("{ owner(id: 11) { nearestDogZones(first: 11) { name } } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
				assertThat(error.getMessage()).isEqualTo("first must be between 1 and 10");
			}));

		verifyNoInteractions(this.dogZones);
	}

	@Test
	void keepsTheOwnerWhenAntwerpCannotBeReached() {
		devOxx();
		given(this.dogZones.findNearest("Groenendaallaan 394", "Antwerpen", 3))
			.willThrow(new UpstreamException(AntwerpDogZoneClient.GEOCODER, "Read timed out"));

		this.graphQl.document("{ owner(id: 11) { lastName nearestDogZones { name } } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getMessage()).isEqualTo("The Antwerp geocoder is unavailable");
				assertThat(error.getErrorType().toString()).isEqualTo("UPSTREAM_UNAVAILABLE");
				assertThat(error.getPath()).isEqualTo("owner.nearestDogZones");
			}))
			.path("owner.lastName")
			.entity(String.class)
			.isEqualTo("Oxx")
			.path("owner.nearestDogZones")
			.valueIsNull();
	}

}
