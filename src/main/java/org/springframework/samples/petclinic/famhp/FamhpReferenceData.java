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
package org.springframework.samples.petclinic.famhp;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.samples.petclinic.famhp.FamhpApi.Resource;

/**
 * The reference data of the FAMHP medicines database, as types of the GraphQL schema. The
 * database sends all of it in one list, and the type of each entry says which list of the
 * schema it belongs to.
 */
final class FamhpReferenceData {

	private FamhpReferenceData() {
	}

	/**
	 * Whether an entry applies to medicines for humans, for animals, or both.
	 */
	enum Usage {

		HUMAN, VETERINARY

	}

	record TargetSpecies(String id, String code, String name) {

		static TargetSpecies from(Resource resource) {
			return new TargetSpecies(resource.id(), resource.code(), resource.label());
		}

	}

	record AuthorisationType(String id, String code, String name) {

		static AuthorisationType from(Resource resource) {
			return new AuthorisationType(resource.id(), resource.code(), resource.label());
		}

	}

	record LegalBasis(String id, String name, List<Usage> usage) {

		static LegalBasis from(Resource resource) {
			return new LegalBasis(resource.id(), resource.label(), usageOf(resource));
		}

	}

	record DeliveryMode(String id, String name) {

		static DeliveryMode from(Resource resource) {
			return new DeliveryMode(resource.id(), resource.label());
		}

	}

	record DocumentType(String id, String code, String name, List<Usage> usage, boolean riskMinimisationMaterial,
			int position) {

		static DocumentType from(Resource resource) {
			return new DocumentType(resource.id(), resource.code(), resource.label(), usageOf(resource),
					Boolean.TRUE.equals(resource.riskMinimisation()), resource.sequence());
		}

	}

	/**
	 * Reads the usage of an entry. A value this class does not know is left out.
	 */
	private static List<Usage> usageOf(Resource resource) {
		if (resource.usage() == null) {
			return List.of();
		}
		return resource.usage().stream().map((usage) -> switch (usage.toLowerCase(Locale.ROOT)) {
			case "human" -> Usage.HUMAN;
			case "veterinary" -> Usage.VETERINARY;
			default -> null;
		}).filter(Objects::nonNull).toList();
	}

}
