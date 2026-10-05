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
package org.springframework.samples.petclinic.rasff;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One RASFF notification with its details, as the <code>RasffNotification</code> type of
 * the GraphQL schema. The nested records are the types of the details. The Jackson
 * annotations map RASFF's names to the names of the schema, and the methods without a
 * component compute the fields that RASFF nests differently.
 * <p>
 * The records with such methods are public. Spring Boot DevTools loads the application
 * classes in a class loader of its own, and graphql-java then calls a method without a
 * component only when its class is public.
 * <p>
 * RASFF also sends the names of the officials who handled a notification. The schema
 * leaves them out.
 *
 * @param id the id
 * @param reference the public reference, for example 2026.8625
 * @param subject what the notification is about
 * @param validationDate the day the European Commission validated the notification
 * @param lastUpdateDate the day of the latest change
 * @param classification the kind of notification, for example an alert
 * @param productType food, feed or food contact material
 * @param basis what led to the notification, for example a consumer complaint
 * @param status the status in RASFF's own words, for example ec_validated
 * @param product the product and what was found in it
 * @param risk how serious the risk is
 * @param countries the countries involved, with the part each one plays
 * @param followUps the follow-ups sent after the notification
 * @param consumerLinksByCountry the websites with consumer information, by country
 */
public record RasffNotification(long id, String reference, String subject,
		@JsonProperty("ecValidationDate") @JsonFormat(pattern = TIMESTAMP) LocalDate validationDate,
		@JsonProperty("lastUpdate") @JsonFormat(pattern = TIMESTAMP) LocalDate lastUpdateDate,
		@JsonProperty("notificationClassification") RasffTerm classification, RasffTerm productType,
		@JsonProperty("notificationBasis") RasffTerm basis, @JsonProperty("notificationStatus") String status,
		Product product, Risk risk, @JsonProperty("organizationFlags") List<CountryInvolvement> countries,
		@JsonProperty("followups") List<FollowUp> followUps,
		@JsonProperty("localConsumerLinksByCountry") Map<String, List<Link>> consumerLinksByCountry) {

	/**
	 * The pattern of RASFF's timestamps, for example 29-09-2026 15:32:19.
	 */
	static final String TIMESTAMP = "dd-MM-yyyy HH:mm:ss";

	public RasffNotification {
		subject = (subject != null) ? subject.strip() : null;
		countries = (countries != null) ? countries : List.of();
		followUps = (followUps != null) ? followUps : List.of();
		consumerLinksByCountry = (consumerLinksByCountry != null) ? consumerLinksByCountry : Map.of();
	}

	/**
	 * The field <code>consumerInformationLinks</code>: the links of every country, in one
	 * list.
	 */
	public List<String> consumerInformationLinks() {
		return this.consumerLinksByCountry.values().stream().flatMap(List::stream).map(Link::link).distinct().toList();
	}

	/**
	 * The product of a notification.
	 *
	 * @param name what the product is, for example cat food
	 * @param category the category of the product
	 * @param distributionStatus whether and where the product was distributed
	 * @param hazards what was found in the product
	 * @param details the packaging and the batches
	 * @param measures what the authorities did about the product
	 */
	record Product(@JsonProperty("description") String name, @JsonProperty("productCategory") RasffTerm category,
			RasffTerm distributionStatus, List<Hazard> hazards,
			@JsonProperty("productDescriptions") List<ProductDetail> details, List<Measure> measures) {

		Product {
			hazards = (hazards != null) ? hazards : List.of();
			details = (details != null) ? details : List.of();
			measures = (measures != null) ? measures : List.of();
		}

	}

	/**
	 * A hazard found in a product, with the measured value and the limit when RASFF gives
	 * them.
	 *
	 * @param hazardCategory the category, which RASFF sends without an id
	 */
	public record Hazard(String name, HazardCategory hazardCategory, String analyticalResult, String unit,
			@JsonProperty("maxPermittedLvl") String maximumPermittedLevel,
			@JsonFormat(pattern = TIMESTAMP) LocalDate samplingDate) {

		public Hazard {
			// names such as "foreign body - foreign bodies" carry double spaces
			name = (name != null) ? name.strip().replaceAll("\\s+", " ") : null;
		}

		/**
		 * The field <code>category</code>.
		 */
		public String category() {
			return (this.hazardCategory != null) ? this.hazardCategory.description() : null;
		}

	}

	record HazardCategory(String description) {

	}

	/**
	 * A description of the product as it was sold: the packaging, the batch and the
	 * weight.
	 */
	record ProductDetail(@JsonProperty("productAspect") String aspect, String labelling, Double weight,
			RasffTerm weightUnit, RasffTerm temperature) {

	}

	/**
	 * A measure an authority took, such as a recall from consumers.
	 *
	 * @param action the measure
	 * @param takenBy the country whose authority took it
	 * @param url a page about the measure, for example a recall notice
	 */
	record Measure(@JsonProperty("actionTaken") RasffTerm action, RasffCountryRef takenBy, String url) {

	}

	/**
	 * The assessment of the risk.
	 *
	 * @param decision how serious the risk is, for example potential risk
	 * @param hazardObserved what was observed, in the words of the notifying country
	 * @param personsAffected the number of people, or animals, who fell ill
	 * @param illness the illness they had
	 */
	record Risk(@JsonProperty("riskDecision") String decision, String hazardObserved,
			@JsonProperty("numberOfPersonsAffected") Integer personsAffected,
			@JsonProperty("typeOfIllness") String illness) {

	}

	/**
	 * A country involved in a notification, with the parts it plays.
	 */
	public record CountryInvolvement(Organisation organization, List<Flag> notificationFlags) {

		/**
		 * The field <code>country</code>.
		 */
		public String country() {
			return this.organization.description();
		}

		/**
		 * The field <code>code</code>.
		 */
		public String code() {
			return this.organization.code();
		}

		/**
		 * The field <code>roles</code>: each part once, in the order RASFF lists them.
		 */
		public List<String> roles() {
			return (this.notificationFlags != null)
					? this.notificationFlags.stream().map(Flag::flagType).distinct().toList() : List.of();
		}

	}

	record Organisation(String description, String code) {

	}

	record Flag(String flagType) {

	}

	/**
	 * A follow-up that a member of the network sent after the notification.
	 *
	 * @param number the number of the follow-up, starting at 1
	 * @param date the day it was sent
	 * @param type what the follow-up reports, for example the outcome of investigations
	 * @param sender the authority that sent it, a country or a region of one
	 * @param senderCode the ISO 3166 alpha-2 code of its country
	 */
	record FollowUp(@JsonProperty("fupNumber") int number,
			@JsonProperty("fupDate") @JsonFormat(pattern = TIMESTAMP) LocalDate date,
			@JsonProperty("fupType") RasffTerm type, @JsonProperty("organizationDescription") String sender,
			@JsonProperty("organizationCode") String senderCode) {

	}

	record Link(String link) {

	}

}
