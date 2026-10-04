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

/**
 * A point in Belgian Lambert 72 (EPSG:31370), the national grid of Belgium. Its
 * coordinates are metres, so the distance between two points is plain Pythagoras.
 *
 * @param x metres east
 * @param y metres north
 */
record LambertPoint(double x, double y) {

	// Belgian Lambert 72 is a Lambert conformal conic projection with two standard
	// parallels (EPSG method 9802) on the International 1924 ellipsoid
	private static final double SEMI_MAJOR_AXIS = 6_378_388.0;

	private static final double FLATTENING = 1 / 297.0;

	private static final double ECCENTRICITY = Math.sqrt(2 * FLATTENING - FLATTENING * FLATTENING);

	private static final double CENTRAL_MERIDIAN = Math.toRadians(4.367486666666666);

	private static final double FIRST_PARALLEL = Math.toRadians(51.16666723333333);

	private static final double SECOND_PARALLEL = Math.toRadians(49.8333339);

	private static final double FALSE_EASTING = 150_000.013;

	private static final double FALSE_NORTHING = 5_400_088.438;

	private static final double N = (Math.log(m(FIRST_PARALLEL)) - Math.log(m(SECOND_PARALLEL)))
			/ (Math.log(t(FIRST_PARALLEL)) - Math.log(t(SECOND_PARALLEL)));

	private static final double F = m(FIRST_PARALLEL) / (N * Math.pow(t(FIRST_PARALLEL), N));

	// the sphere that Web Mercator (EPSG:3857) projects onto
	private static final double WEB_MERCATOR_RADIUS = 6_378_137.0;

	double distanceTo(LambertPoint other) {
		return Math.hypot(this.x - other.x, this.y - other.y);
	}

	/**
	 * Reads a point from an answer of the Antwerp dog zone map over SOAP.
	 * <p>
	 * The map stores its zones in Lambert 72 and answers in Web Mercator, and it ignores
	 * a request for any other coordinate system. On the way it skips the shift from the
	 * Belgian 1972 datum to WGS84: it reads the Belgian latitude and longitude of a zone
	 * as if they were WGS84. Every zone then lands about 107 m north-west of where the
	 * city's REST interface and its geocoder put it. This method takes the same steps
	 * backwards, from Web Mercator to latitude and longitude, and from there to Lambert
	 * 72 on the Belgian ellipsoid.
	 */
	static LambertPoint fromDogZoneMap(double webMercatorX, double webMercatorY) {
		double latitude = 2 * Math.atan(Math.exp(webMercatorY / WEB_MERCATOR_RADIUS)) - Math.PI / 2;
		double longitude = webMercatorX / WEB_MERCATOR_RADIUS;
		double r = SEMI_MAJOR_AXIS * F * Math.pow(t(latitude), N);
		double theta = N * (longitude - CENTRAL_MERIDIAN);
		return new LambertPoint(FALSE_EASTING + r * Math.sin(theta), FALSE_NORTHING - r * Math.cos(theta));
	}

	private static double m(double latitude) {
		double sine = ECCENTRICITY * Math.sin(latitude);
		return Math.cos(latitude) / Math.sqrt(1 - sine * sine);
	}

	private static double t(double latitude) {
		double sine = ECCENTRICITY * Math.sin(latitude);
		return Math.tan(Math.PI / 4 - latitude / 2) / Math.pow((1 - sine) / (1 + sine), ECCENTRICITY / 2);
	}

}
