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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A country that a notification can name as the origin of a product or as a country the
 * product was distributed to.
 *
 * @param id the id, which the filters of a search take
 * @param name the English short name
 * @param code the ISO 3166 alpha-2 code
 */
record RasffCountry(long id, @JsonProperty("englishShortName") String name, @JsonProperty("alpha2Code") String code) {

}
