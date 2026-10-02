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
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.PetValidator;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Answers the mutations of the GraphQL schema. Each mutation does what the matching web
 * form does, through the same repositories, entities and validators.
 * <p>
 * The constraints on the entities and the {@link PetValidator} are reused as they are.
 * Three rules live in the web controllers, where a mutation cannot reach them, so they
 * are repeated here: a pet name is unique for its owner, a birth date is today or
 * earlier, and a visit is booked for a day after today.
 * <p>
 * A mutation runs in one transaction, so the owner it loads stays attached to the
 * persistence context. A new pet or visit is added to that owner and written by a flush,
 * which gives the same object its id. Saving the owner would merge it, and a merge copies
 * a new pet or visit and leaves the original without an id. A failed validation throws an
 * unchecked exception, which rolls the transaction back.
 */
@Controller
@Transactional
class PetclinicGraphQlMutationController {

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	private final Validator validator;

	PetclinicGraphQlMutationController(OwnerRepository owners, PetTypeRepository petTypes, Validator validator) {
		this.owners = owners;
		this.petTypes = petTypes;
		this.validator = validator;
	}

	@MutationMapping
	Owner addOwner(@Argument AddOwnerInput input) {
		Owner owner = new Owner();
		setDetails(owner, input.firstName(), input.lastName(), input.address(), input.city(), input.telephone());
		return save(owner);
	}

	@MutationMapping
	Owner updateOwner(@Argument UpdateOwnerInput input) {
		Owner owner = owner(input.id());
		setDetails(owner, input.firstName(), input.lastName(), input.address(), input.city(), input.telephone());
		return save(owner);
	}

	@MutationMapping
	Pet addPet(@Argument AddPetInput input) {
		Owner owner = owner(input.ownerId());
		boolean nameTaken = owner.getPet(input.name(), true) != null;
		Pet pet = new Pet();
		setDetails(pet, input.name(), input.birthDate(), petType(input.typeId()));
		validate(pet, nameTaken);
		owner.addPet(pet);
		flush(pet);
		return pet;
	}

	@MutationMapping
	Pet updatePet(@Argument UpdatePetInput input) {
		Owner owner = ownerOfPet(input.id());
		Pet pet = owner.getPet(toId(input.id()).orElseThrow());
		Pet sameName = owner.getPet(input.name(), false);
		boolean nameTaken = sameName != null && !Objects.equals(sameName.getId(), pet.getId());
		setDetails(pet, input.name(), input.birthDate(), petType(input.typeId()));
		validate(pet, nameTaken);
		flush(pet);
		return pet;
	}

	@MutationMapping
	Visit addVisit(@Argument AddVisitInput input) {
		Owner owner = ownerOfPet(input.petId());
		// a new visit is dated tomorrow, which is what the web form proposes
		Visit visit = new Visit();
		if (input.date() != null) {
			visit.setDate(input.date());
		}
		visit.setDescription(input.description());
		Errors errors = new BeanPropertyBindingResult(visit, "visit");
		this.validator.validate(visit, errors);
		if (!visit.getDate().isAfter(LocalDate.now())) {
			errors.rejectValue("date", "typeMismatch.visitDate", "must be after today");
		}
		failOnErrors(errors);
		owner.addVisit(toId(input.petId()).orElseThrow(), visit);
		this.owners.flush();
		return visit;
	}

	private static void setDetails(Owner owner, String firstName, String lastName, String address, String city,
			String telephone) {
		owner.setFirstName(firstName);
		owner.setLastName(lastName);
		owner.setAddress(address);
		owner.setCity(city);
		owner.setTelephone(telephone);
	}

	private static void setDetails(Pet pet, String name, LocalDate birthDate, PetType type) {
		pet.setName(name);
		pet.setBirthDate(birthDate);
		pet.setType(type);
	}

	private Owner save(Owner owner) {
		Errors errors = new BeanPropertyBindingResult(owner, "owner");
		this.validator.validate(owner, errors);
		failOnErrors(errors);
		return this.owners.save(owner);
	}

	private void validate(Pet pet, boolean nameTaken) {
		Errors errors = new BeanPropertyBindingResult(pet, "pet");
		new PetValidator().validate(pet, errors);
		if (nameTaken) {
			errors.rejectValue("name", "duplicate", "already exists");
		}
		if (pet.getBirthDate() != null && pet.getBirthDate().isAfter(LocalDate.now())) {
			errors.rejectValue("birthDate", "future", "must be today or earlier");
		}
		failOnErrors(errors);
	}

	/**
	 * Writes the new or changed pet of the attached owner. The database has a unique
	 * constraint on the name of a pet for an owner, which catches two requests that add
	 * the same name at the same time.
	 */
	private void flush(Pet pet) {
		try {
			this.owners.flush();
		}
		catch (DataIntegrityViolationException ex) {
			String message = ex.getMessage();
			if (message == null || !message.toLowerCase().contains("unique_owner_pet_name")) {
				throw ex;
			}
			Errors errors = new BeanPropertyBindingResult(pet, "pet");
			errors.rejectValue("name", "duplicate", "already exists");
			throw new InvalidInputException(errors);
		}
	}

	private static void failOnErrors(Errors errors) {
		if (errors.hasErrors()) {
			throw new InvalidInputException(errors);
		}
	}

	private Owner owner(String id) {
		return toId(id).flatMap(this.owners::findById).orElseThrow(() -> new UnknownIdException("Owner", id));
	}

	private Owner ownerOfPet(String petId) {
		return toId(petId).flatMap(this.owners::findByPetsId).orElseThrow(() -> new UnknownIdException("Pet", petId));
	}

	private PetType petType(String id) {
		return toId(id).flatMap(this.petTypes::findById).orElseThrow(() -> new UnknownIdException("Pet type", id));
	}

	/**
	 * A GraphQL ID arrives as text. An id that is not a valid integer cannot belong to an
	 * object, so it is treated as an unknown id.
	 */
	private static Optional<Integer> toId(String id) {
		try {
			return Optional.of(Integer.valueOf(id));
		}
		catch (NumberFormatException ex) {
			return Optional.empty();
		}
	}

}
