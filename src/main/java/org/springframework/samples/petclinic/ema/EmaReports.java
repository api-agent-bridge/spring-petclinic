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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.upstream.RestUpstreams;
import org.springframework.stereotype.Component;

/**
 * Keeps the records of the EMA reports in memory. EMA publishes each report as one file
 * with every record of a table, up to 34 MB for the document lists, so a search means
 * downloading the whole file. EMA refreshes the files twice a day. A report is downloaded
 * the first time a query needs it, and again when a query needs it after the refresh
 * period.
 */
@Component
class EmaReports {

	static final String EMA = "The European Medicines Agency website";

	private static final Log logger = LogFactory.getLog(EmaReports.class);

	private final Duration refreshAfter;

	private final CachedReport<EmaMedicine> medicines;

	private final CachedReport<EmaDocument> eparDocuments;

	private final CachedReport<EmaDocument> otherDocuments;

	private final CachedReport<EmaEvent> events;

	private final CachedReport<EmaWebsiteArticle> websiteArticles;

	private final CachedReport<EmaOutsideEuOpinion> outsideEuOpinions;

	private final CachedReport<EmaHerbalSubstance> herbalSubstances;

	private final CachedReport<EmaMaximumResidueLimit> maximumResidueLimits;

	private final CachedReport<EmaOrphanDesignation> orphanDesignations;

	private final CachedReport<EmaPaediatricInvestigationPlan> paediatricInvestigationPlans;

	private final CachedReport<EmaPeriodicSafetyAssessment> periodicSafetyAssessments;

	private final CachedReport<EmaPostAuthorisationProcedure> postAuthorisationProcedures;

	private final CachedReport<EmaNewsItem> news;

	private final CachedReport<EmaReferral> referrals;

	private final CachedReport<EmaShortage> shortages;

	private final CachedReport<EmaSafetyCommunication> safetyCommunications;

	EmaReports(EmaApi ema, @Value("${petclinic.upstream.ema.refresh-after}") Duration refreshAfter) {
		this.refreshAfter = refreshAfter;
		this.medicines = new CachedReport<>(ema, "medicines-output-medicines_json-report_en", EmaMedicine::from);
		this.eparDocuments = new CachedReport<>(ema, "documents-output-epar_documents_json-report_en",
				EmaDocument::from);
		this.otherDocuments = new CachedReport<>(ema, "documents-output-non_epar_documents_json-report_en",
				EmaDocument::from);
		this.events = new CachedReport<>(ema, "events-json-report_en", EmaEvent::from);
		this.websiteArticles = new CachedReport<>(ema, "general-json-report_en", EmaWebsiteArticle::from);
		this.outsideEuOpinions = new CachedReport<>(ema, "medicine-use-outside-eu-output-json-report_en",
				EmaOutsideEuOpinion::from);
		this.herbalSubstances = new CachedReport<>(ema, "medicines-output-herbal_medicines-report-output-json_en",
				EmaHerbalSubstance::from);
		this.maximumResidueLimits = new CachedReport<>(ema, "medicines-output-maximum_residue_limits-json-report_en",
				EmaMaximumResidueLimit::from);
		this.orphanDesignations = new CachedReport<>(ema, "medicines-output-orphan_designations-json-report_en",
				EmaOrphanDesignation::from);
		this.paediatricInvestigationPlans = new CachedReport<>(ema,
				"medicines-output-paediatric_investigation_plans-output-json-report_en",
				EmaPaediatricInvestigationPlan::from);
		this.periodicSafetyAssessments = new CachedReport<>(ema,
				"medicines-output-periodic_safety_update_report_single_assessments-output-json-report_en",
				EmaPeriodicSafetyAssessment::from);
		this.postAuthorisationProcedures = new CachedReport<>(ema, "medicines-output-post_authorisation_json-report_en",
				EmaPostAuthorisationProcedure::from);
		this.news = new CachedReport<>(ema, "news-json-report_en", EmaNewsItem::from);
		this.referrals = new CachedReport<>(ema, "referrals-output-json-report_en", EmaReferral::from);
		this.shortages = new CachedReport<>(ema, "shortages-output-json-report_en", EmaShortage::from);
		this.safetyCommunications = new CachedReport<>(ema, "dhpc-output-json-report_en", EmaSafetyCommunication::from);
	}

	List<EmaMedicine> medicines() {
		return this.medicines.records();
	}

	List<EmaDocument> eparDocuments() {
		return this.eparDocuments.records();
	}

	List<EmaDocument> otherDocuments() {
		return this.otherDocuments.records();
	}

	List<EmaEvent> events() {
		return this.events.records();
	}

	List<EmaWebsiteArticle> websiteArticles() {
		return this.websiteArticles.records();
	}

	List<EmaOutsideEuOpinion> outsideEuOpinions() {
		return this.outsideEuOpinions.records();
	}

	List<EmaHerbalSubstance> herbalSubstances() {
		return this.herbalSubstances.records();
	}

	List<EmaMaximumResidueLimit> maximumResidueLimits() {
		return this.maximumResidueLimits.records();
	}

	List<EmaOrphanDesignation> orphanDesignations() {
		return this.orphanDesignations.records();
	}

	List<EmaPaediatricInvestigationPlan> paediatricInvestigationPlans() {
		return this.paediatricInvestigationPlans.records();
	}

	List<EmaPeriodicSafetyAssessment> periodicSafetyAssessments() {
		return this.periodicSafetyAssessments.records();
	}

	List<EmaPostAuthorisationProcedure> postAuthorisationProcedures() {
		return this.postAuthorisationProcedures.records();
	}

	List<EmaNewsItem> news() {
		return this.news.records();
	}

	List<EmaReferral> referrals() {
		return this.referrals.records();
	}

	List<EmaShortage> shortages() {
		return this.shortages.records();
	}

	List<EmaSafetyCommunication> safetyCommunications() {
		return this.safetyCommunications.records();
	}

	/**
	 * One report and the records read from its latest download.
	 */
	private final class CachedReport<T extends EmaPublished> {

		private final EmaApi ema;

		private final String name;

		private final Function<EmaRecord, T> reader;

		private List<T> records;

		private Instant downloaded;

		CachedReport(EmaApi ema, String name, Function<EmaRecord, T> reader) {
			this.ema = ema;
			this.name = name;
			this.reader = reader;
		}

		/**
		 * Returns the records. The first call downloads the report, and so does a call
		 * after the refresh period. Queries that need the same report wait for one
		 * download.
		 */
		synchronized List<T> records() {
			if (this.records == null
					|| Duration.between(this.downloaded, Instant.now()).compareTo(EmaReports.this.refreshAfter) > 0) {
				long started = System.nanoTime();
				EmaApi.Report report = RestUpstreams.call(EMA, () -> this.ema.report(this.name));
				Stream<T> records = (report.data() != null)
						? report.data().stream().map(EmaRecord::new).map(this.reader) : Stream.empty();
				this.records = records.sorted(EmaPublished.LATEST_FIRST).toList();
				this.downloaded = Instant.now();
				logger.info("EMA report %s holds %d records, kept for %s (%d ms)".formatted(this.name,
						this.records.size(), EmaReports.this.refreshAfter,
						Duration.ofNanos(System.nanoTime() - started).toMillis()));
			}
			return this.records;
		}

	}

}
