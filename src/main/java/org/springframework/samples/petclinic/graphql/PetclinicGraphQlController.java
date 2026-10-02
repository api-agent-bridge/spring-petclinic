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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Controller;

/**
 * Answers the queries of the GraphQL schema from the existing repositories. Pets, visits
 * and specialties come straight from the getters of the entities the repositories return.
 */
@Controller
class PetclinicGraphQlController {

	/**
	 * Largest page a client may ask for. The web pages use 5.
	 */
	static final int MAX_PAGE_SIZE = 50;

	/**
	 * Order of the owners across pages. SQL leaves the order of rows open unless the
	 * query sorts them, and paging needs the same order on every request.
	 */
	static final Sort OWNER_ORDER = Sort.by("id");

	private final OwnerRepository owners;

	private final VetRepository vets;

	private final PetTypeRepository petTypes;

	PetclinicGraphQlController(OwnerRepository owners, VetRepository vets, PetTypeRepository petTypes) {
		this.owners = owners;
		this.vets = vets;
		this.petTypes = petTypes;
	}

	@QueryMapping
	OwnerPage owners(@Argument String lastName, @Argument int page, @Argument int size) {
		String prefix = (lastName != null) ? lastName.strip() : "";
		// page and size come from the client, so both are clamped before they reach
		// the database
		int pageIndex = Math.max(page, 1) - 1;
		int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		return OwnerPage
			.of(this.owners.findByLastNameStartingWith(prefix, PageRequest.of(pageIndex, pageSize, OWNER_ORDER)));
	}

	@QueryMapping
	Optional<Owner> owner(@Argument String id) {
		// a GraphQL ID arrives as text; an id that is not a valid integer cannot match
		// an owner, so the answer is the same as for an unknown id
		try {
			return this.owners.findById(Integer.valueOf(id));
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

	@QueryMapping
	Collection<Vet> vets() {
		return this.vets.findAll();
	}

	@QueryMapping
	List<PetType> petTypes() {
		return this.petTypes.findPetTypes();
	}

}
