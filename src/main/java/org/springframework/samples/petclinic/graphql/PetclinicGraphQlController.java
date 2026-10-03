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
import org.springframework.samples.petclinic.owner.PetRepository;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.stereotype.Controller;

/**
 * Answers the queries of the GraphQL schema from the repositories.
 * <p>
 * The pets, visits and specialties inside the objects they return come straight from the
 * getters of the entities. Petclinic maps them as eager collections, and
 * {@code hibernate.default_batch_fetch_size} loads each level for up to 16 parents in one
 * query. A page of ten owners with their pets and visits takes four SQL queries, and it
 * takes the same four when the GraphQL query selects only the owner ids.
 * <p>
 * A GraphQL API built from scratch would make those collections lazy and load them with
 * {@code @BatchMapping}, which runs one query per level and only for the fields a query
 * selects. This demo leaves the mapping as it is on purpose: lazy collections would
 * change how the web pages load their data, and the demo keeps its changes to Petclinic
 * small.
 */
@Controller
class PetclinicGraphQlController {

	/**
	 * Largest page a client may ask for. The web pages use 5.
	 */
	static final int MAX_PAGE_SIZE = 50;

	/**
	 * Order of the owners and pets across pages. SQL leaves the order of rows open unless
	 * the query sorts them, and paging needs the same order on every request.
	 */
	static final Sort ID_ORDER = Sort.by("id");

	private final OwnerRepository owners;

	private final PetRepository pets;

	private final VetRepository vets;

	private final PetTypeRepository petTypes;

	PetclinicGraphQlController(OwnerRepository owners, PetRepository pets, VetRepository vets,
			PetTypeRepository petTypes) {
		this.owners = owners;
		this.pets = pets;
		this.vets = vets;
		this.petTypes = petTypes;
	}

	@QueryMapping
	OwnerPage owners(@Argument String firstName, @Argument String lastName, @Argument int page, @Argument int size) {
		return OwnerPage.of(this.owners.findByFirstNameStartingWithAndLastNameStartingWithAllIgnoreCase(
				stripped(firstName), stripped(lastName), pageRequest(page, size)));
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
	PetPage pets(@Argument String name, @Argument String type, @Argument int page, @Argument int size) {
		String typeName = stripped(type);
		PageRequest pageRequest = pageRequest(page, size);
		// a derived query cannot leave out one of its conditions, so an empty type uses
		// the query without the type
		return PetPage.of(typeName.isEmpty() ? this.pets.findByNameStartingWithIgnoreCase(stripped(name), pageRequest)
				: this.pets.findByNameStartingWithAndTypeNameAllIgnoreCase(stripped(name), typeName, pageRequest));
	}

	@QueryMapping
	Collection<Vet> vets() {
		return this.vets.findAll();
	}

	@QueryMapping
	List<PetType> petTypes() {
		return this.petTypes.findPetTypes();
	}

	// page and size come from the client, so both are clamped before they reach the
	// database
	private static PageRequest pageRequest(int page, int size) {
		int pageIndex = Math.max(page, 1) - 1;
		int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		return PageRequest.of(pageIndex, pageSize, ID_ORDER);
	}

	// the text arguments are nullable, and an explicit null matches the same as an
	// empty text
	private static String stripped(String text) {
		return (text != null) ? text.strip() : "";
	}

}
