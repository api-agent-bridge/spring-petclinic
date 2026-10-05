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
package org.springframework.samples.petclinic.ema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.samples.petclinic.upstream.ResultPages;
import org.springframework.samples.petclinic.upstream.SearchText;
import org.springframework.stereotype.Controller;

/**
 * Answers the <code>ema*</code> queries of the GraphQL schema from the EMA reports. Each
 * query filters the records of one report and returns one page of them. The search
 * argument matches the names and other texts that identify a record, and the other
 * arguments filter on one field each.
 */
@Controller
class EmaGraphQlController {

	private final EmaReports reports;

	EmaGraphQlController(EmaReports reports) {
		this.reports = reports;
	}

	@QueryMapping
	EmaMedicine.Page emaMedicines(@Argument String search, @Argument EmaCategory category, @Argument String status,
			@Argument String species, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		SearchText speciesText = SearchText.of(species);
		return ResultPages.page(filter(this.reports.medicines(),
				(medicine) -> text
					.matches(texts(medicine.name(), medicine.commonNames(), medicine.activeSubstances(),
							medicine.therapeuticAreas()))
						&& (category == null || category == medicine.category()) && sameText(status, medicine.status())
						&& speciesText.matches(medicine.targetSpecies())),
				page, size, EmaMedicine.Page::new);
	}

	@QueryMapping
	EmaDocument.Page emaEparDocuments(@Argument String search, @Argument String productNumber, @Argument String type,
			@Argument int page, @Argument int size) {
		return documents(this.reports.eparDocuments(), search, type, page, size,
				(document) -> sameText(productNumber, document.productNumber()));
	}

	@QueryMapping
	EmaDocument.Page emaDocuments(@Argument String search, @Argument String type, @Argument int page,
			@Argument int size) {
		return documents(this.reports.otherDocuments(), search, type, page, size, (document) -> true);
	}

	@QueryMapping
	EmaEvent.Page emaEvents(@Argument String search, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(filter(this.reports.events(), (event) -> text.matches(event.title(), event.location())),
				page, size, EmaEvent.Page::new);
	}

	@QueryMapping
	EmaWebsiteArticle.Page emaWebsiteArticles(@Argument String search, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.websiteArticles(), (article) -> text.matches(article.title(), article.summary())),
				page, size, EmaWebsiteArticle.Page::new);
	}

	@QueryMapping
	EmaOutsideEuOpinion.Page emaOutsideEuOpinions(@Argument String search, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.outsideEuOpinions(),
						(opinion) -> text.matches(texts(opinion.name(), opinion.activeSubstances(),
								opinion.commonNames(), opinion.therapeuticAreas()))),
				page, size, EmaOutsideEuOpinion.Page::new);
	}

	@QueryMapping
	EmaHerbalSubstance.Page emaHerbalSubstances(@Argument String search, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.herbalSubstances(),
						(substance) -> text.matches(
								texts(substance.latinName(), substance.englishName(), substance.botanicalNames()))),
				page, size, EmaHerbalSubstance.Page::new);
	}

	@QueryMapping
	EmaMaximumResidueLimit.Page emaMaximumResidueLimits(@Argument String search, @Argument String species,
			@Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		SearchText speciesText = SearchText.of(species);
		return ResultPages.page(
				filter(this.reports.maximumResidueLimits(),
						(limit) -> text.matches(limit.title(), limit.activeSubstance())
								&& speciesText.matches(limit.targetSpecies())),
				page, size, EmaMaximumResidueLimit.Page::new);
	}

	@QueryMapping
	EmaOrphanDesignation.Page emaOrphanDesignations(@Argument String search, @Argument String status,
			@Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.orphanDesignations(),
						(designation) -> text.matches(texts(designation.medicineNames(), designation.activeSubstances(),
								designation.intendedUse())) && sameText(status, designation.status())),
				page, size, EmaOrphanDesignation.Page::new);
	}

	@QueryMapping
	EmaPaediatricInvestigationPlan.Page emaPaediatricInvestigationPlans(@Argument String search, @Argument int page,
			@Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.paediatricInvestigationPlans(),
						(plan) -> text
							.matches(texts(plan.activeSubstances(), plan.inventedNames(), plan.conditions()))),
				page, size, EmaPaediatricInvestigationPlan.Page::new);
	}

	@QueryMapping
	EmaPeriodicSafetyAssessment.Page emaPeriodicSafetyAssessments(@Argument String search, @Argument int page,
			@Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.periodicSafetyAssessments(),
						(assessment) -> text.matches(texts(assessment.activeSubstances(),
								assessment.substancesInScope(), assessment.procedureNumber()))),
				page, size, EmaPeriodicSafetyAssessment.Page::new);
	}

	@QueryMapping
	EmaPostAuthorisationProcedure.Page emaPostAuthorisationProcedures(@Argument String search,
			@Argument EmaCategory category, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.postAuthorisationProcedures(),
						(procedure) -> text
							.matches(texts(procedure.name(), procedure.activeSubstances(), procedure.commonNames()))
								&& (category == null || category == procedure.category())),
				page, size, EmaPostAuthorisationProcedure.Page::new);
	}

	@QueryMapping
	EmaNewsItem.Page emaNews(@Argument String search, @Argument String topic, @Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		SearchText topicText = SearchText.of(topic);
		return ResultPages.page(filter(this.reports.news(),
				(item) -> text.matches(texts(item.title(), item.summary(), item.relatedMedicines()))
						&& topicText.matches(item.topics())),
				page, size, EmaNewsItem.Page::new);
	}

	@QueryMapping
	EmaReferral.Page emaReferrals(@Argument String search, @Argument EmaCategory category, @Argument int page,
			@Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.referrals(),
						(referral) -> text
							.matches(texts(referral.name(), referral.commonNames(),
									referral.centrallyAuthorisedMedicines(), referral.nationallyAuthorisedMedicines()))
								&& (category == null || category == referral.category())),
				page, size, EmaReferral.Page::new);
	}

	@QueryMapping
	EmaShortage.Page emaShortages(@Argument String search, @Argument String status, @Argument int page,
			@Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(filter(this.reports.shortages(),
				(shortage) -> text.matches(texts(shortage.medicine(), shortage.commonNames()))
						&& sameText(status, shortage.status())),
				page, size, EmaShortage.Page::new);
	}

	@QueryMapping
	EmaSafetyCommunication.Page emaSafetyCommunications(@Argument String search, @Argument EmaCategory category,
			@Argument int page, @Argument int size) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(this.reports.safetyCommunications(),
						(communication) -> text
							.matches(texts(communication.medicine(), communication.activeSubstances(),
									communication.otherMedicines()))
								&& (category == null || category == communication.category())),
				page, size, EmaSafetyCommunication.Page::new);
	}

	private static EmaDocument.Page documents(List<EmaDocument> documents, String search, String type, int page,
			int size, Predicate<EmaDocument> condition) {
		SearchText text = SearchText.of(search);
		return ResultPages.page(
				filter(documents,
						(document) -> text.matches(document.name(), document.medicineName(), document.referenceNumber())
								&& sameText(type, document.type()) && condition.test(document)),
				page, size, EmaDocument.Page::new);
	}

	private static <T> List<T> filter(List<T> records, Predicate<T> condition) {
		return records.stream().filter(condition).toList();
	}

	/**
	 * Whether a value equals the filter, in upper or lower case. A null or blank filter
	 * matches every value.
	 */
	private static boolean sameText(String filter, String value) {
		return filter == null || filter.isBlank() || filter.strip().equalsIgnoreCase(value);
	}

	/**
	 * Puts the texts and lists of texts of a record into one list, for a search.
	 */
	private static List<String> texts(Object... values) {
		List<String> texts = new ArrayList<>();
		for (Object value : values) {
			if (value instanceof String text) {
				texts.add(text);
			}
			else if (value instanceof Collection<?> list) {
				list.forEach((item) -> texts.add((String) item));
			}
		}
		return texts;
	}

}
