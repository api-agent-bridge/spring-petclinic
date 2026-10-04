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

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.upstream.InvalidArgumentException;
import org.springframework.stereotype.Controller;

/**
 * Adds the dog zones of Antwerp nearest to an owner's address to the GraphQL schema, as a
 * field of <code>Owner</code>.
 */
@Controller
class DogZoneGraphQlController {

	static final int MAX_ZONES = 10;

	private final AntwerpDogZoneClient dogZones;

	DogZoneGraphQlController(AntwerpDogZoneClient dogZones) {
		this.dogZones = dogZones;
	}

	/**
	 * Resolves <code>Owner.nearestDogZones</code>. An owner whose address is written
	 * street first costs one call to the geocoder, and one call to the dog zone map when
	 * the geocoder finds the address.
	 */
	@SchemaMapping(typeName = "Owner")
	List<DogZone> nearestDogZones(Owner owner, @Argument int first) {
		if (first < 1 || first > MAX_ZONES) {
			throw new InvalidArgumentException("first must be between 1 and " + MAX_ZONES);
		}
		return this.dogZones.findNearest(owner.getAddress(), owner.getCity(), first);
	}

}
