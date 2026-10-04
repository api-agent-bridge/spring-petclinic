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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests the {@link LambertPoint} with positions of the Groenendaallaan dog zone in
 * Merksem, read from the city's services on 2026-10-04.
 */
class LambertPointTests {

	@Test
	void readsAPointOfTheDogZoneMapBackIntoLambert72() {
		// the point the map answers over SOAP, in Web Mercator
		LambertPoint zone = LambertPoint.fromDogZoneMap(492131.4, 6665169.1);

		// the position the city's REST interface gives for the same zone in Lambert 72
		assertThat(zone.x()).isCloseTo(153729.204, within(0.5));
		assertThat(zone.y()).isCloseTo(215216.078, within(0.5));
	}

	@Test
	void measuresTheDistanceInMetres() {
		LambertPoint address = new LambertPoint(153324.4, 215113.8);

		assertThat(address.distanceTo(new LambertPoint(153624.4, 215513.8))).isEqualTo(500.0, within(0.001));
	}

}
