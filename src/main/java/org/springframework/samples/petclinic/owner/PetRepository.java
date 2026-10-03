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

package org.springframework.samples.petclinic.owner;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository class for <code>Pet</code> domain objects. The web pages reach a pet through
 * its owner, and this repository finds pets across every owner.
 */
public interface PetRepository extends JpaRepository<Pet, Integer> {

	/**
	 * Retrieve {@link Pet}s from the data store whose name <i>starts</i> with the given
	 * name, ignoring case. An empty value matches every name.
	 * @param name Value the name starts with
	 * @param pageable the page to return
	 * @return a page of matching {@link Pet}s
	 */
	Page<Pet> findByNameStartingWithIgnoreCase(String name, Pageable pageable);

	/**
	 * Retrieve {@link Pet}s of one type from the data store whose name <i>starts</i> with
	 * the given name, ignoring case in both names. An empty name matches every name.
	 * @param name Value the name starts with
	 * @param typeName the name of the {@link PetType}, for example cat
	 * @param pageable the page to return
	 * @return a page of matching {@link Pet}s
	 */
	Page<Pet> findByNameStartingWithAndTypeNameAllIgnoreCase(String name, String typeName, Pageable pageable);

}
