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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.famhp.FamhpApi.Resource;
import org.springframework.samples.petclinic.famhp.FamhpReferenceData.AuthorisationType;
import org.springframework.samples.petclinic.famhp.FamhpReferenceData.DeliveryMode;
import org.springframework.samples.petclinic.famhp.FamhpReferenceData.DocumentType;
import org.springframework.samples.petclinic.famhp.FamhpReferenceData.LegalBasis;
import org.springframework.samples.petclinic.famhp.FamhpReferenceData.TargetSpecies;
import org.springframework.samples.petclinic.famhp.FamhpReferenceData.Usage;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.samples.petclinic.upstream.SearchText;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>famhp*</code> queries of the GraphQL schema from the reference data
 * of the FAMHP medicines database. Every query costs one call, which returns all the
 * reference data, and keeps the entries of one type.
 */
@Controller
class FamhpGraphQlController {

	static final String FAMHP = "The FAMHP medicines database";

	private final FamhpApi famhp;

	FamhpGraphQlController(FamhpApi famhp) {
		this.famhp = famhp;
	}

	@QueryMapping
	List<TargetSpecies> famhpTargetSpecies(@Argument String name) {
		SearchText search = SearchText.of(name);
		return resources("targetSpecies").map(TargetSpecies::from)
			.filter((species) -> search.matches(species.name()))
			.sorted(Comparator.comparing(TargetSpecies::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	@QueryMapping
	List<AuthorisationType> famhpAuthorisationTypes() {
		return resources("authorisationType").map(AuthorisationType::from)
			.sorted(Comparator.comparing(AuthorisationType::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	@QueryMapping
	List<LegalBasis> famhpLegalBases(@Argument Usage usage) {
		return resources("legalBasis").map(LegalBasis::from)
			.filter((basis) -> usage == null || basis.usage().contains(usage))
			.sorted(Comparator.comparing(LegalBasis::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	@QueryMapping
	List<DeliveryMode> famhpDeliveryModes() {
		return resources("deliveryModus").map(DeliveryMode::from)
			.sorted(Comparator.comparing(DeliveryMode::name, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

	@QueryMapping
	List<DocumentType> famhpDocumentTypes() {
		return resources("documentType").map(DocumentType::from)
			.sorted(Comparator.comparingInt(DocumentType::position))
			.toList();
	}

	private Stream<Resource> resources(String type) {
		return RestUpstreams.call(FAMHP, this.famhp::resources)
			.stream()
			.filter((resource) -> type.equals(resource.type()));
	}

}
