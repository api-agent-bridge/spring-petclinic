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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.samples.petclinic.upstream.UpstreamException;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceException;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Finds the off-leash dog zones of the City of Antwerp nearest to an address. Both calls
 * go to the city's ArcGIS Server over SOAP:
 * <ol>
 * <li><code>GeocodeAddress</code> on the city's geocoder turns the address into a point
 * in Belgian Lambert 72, whose coordinates are metres.</li>
 * <li><code>QueryFeatureData</code> on the dog zone map returns the zones inside a square
 * around that point.</li>
 * </ol>
 * The map's WSDL declares 110 operations in 290 KB, and this client needs one operation
 * of each service, so it writes both requests by hand and reads the answers with DOM.
 */
@Component
class AntwerpDogZoneClient {

	static final String GEOCODER = "The Antwerp geocoder";

	static final String MAP = "The Antwerp dog zone service";

	private static final Log logger = LogFactory.getLog(AntwerpDogZoneClient.class);

	// the two services of the same server use different versions of Esri's schema
	private static final String GEOCODER_NAMESPACE = "http://www.esri.com/schemas/ArcGIS/10.8";

	private static final String MAP_NAMESPACE = "http://www.esri.com/schemas/ArcGIS/3.5.0";

	/**
	 * The fields to return. The layer has 30.
	 */
	private static final String FIELDS = "id,naam,straatnaam,postcode,district,opp_ha,netheid,verlichting,"
			+ "hoogte_afsluiting,materiaal_afsluiting,aantal_vuilnisbakken,aantal_zitbanken,type_ondergrond";

	private static final String SHAPE = "shape";

	private static final int LAMBERT_72 = 31370;

	/**
	 * A Belgian address names the street first, for example "Groenendaallaan 394" or
	 * "Grote Markt 1A". An address that starts with the house number, like those of the
	 * sample owners in Wisconsin, does not reach the geocoder.
	 */
	private static final Pattern STREET_AND_NUMBER = Pattern.compile("(\\p{L}[^\\d]*?)\\s+(\\d+\\S*)");

	private final WebServiceTemplate soap;

	private final String mapUrl;

	private final String mapName;

	private final int layerId;

	private final String geocoderUrl;

	private final int searchRadiusInMetres;

	/**
	 * Builds the template with the builder Spring Boot configures, which picks the HTTP
	 * client from the classpath.
	 */
	AntwerpDogZoneClient(WebServiceTemplateBuilder builder, @Value("${petclinic.upstream.antwerp.url}") String mapUrl,
			@Value("${petclinic.upstream.antwerp.map-name}") String mapName,
			@Value("${petclinic.upstream.antwerp.layer-id}") int layerId,
			@Value("${petclinic.upstream.antwerp.geocoder-url}") String geocoderUrl,
			@Value("${petclinic.upstream.antwerp.search-radius-in-metres}") int searchRadiusInMetres,
			@Value("${petclinic.upstream.connect-timeout}") Duration connectTimeout,
			@Value("${petclinic.upstream.read-timeout}") Duration readTimeout) {
		this.mapUrl = mapUrl;
		this.mapName = mapName;
		this.layerId = layerId;
		this.geocoderUrl = geocoderUrl;
		this.searchRadiusInMetres = searchRadiusInMetres;
		HttpClientSettings timeouts = HttpClientSettings.defaults()
			.withConnectTimeout(connectTimeout)
			.withReadTimeout(readTimeout);
		// ArcGIS Server sends SOAP faults with HTTP status 200. By default the
		// template looks for a fault only when the status is 500, and would hand
		// the fault back as if it were a result. With checkConnectionForFault
		// off, it inspects every response body.
		this.soap = builder.httpMessageSenderFactory(WebServiceMessageSenderFactory.http(timeouts))
			.setCheckConnectionForFault(false)
			.build();
	}

	/**
	 * Returns the dog zones within the search radius of an address, nearest first. The
	 * list is empty when the address is not written street first, or when the geocoder
	 * cannot find it.
	 * @param address the street and house number, for example "Groenendaallaan 394"
	 * @param city the city, for example "Antwerpen"
	 * @param first how many zones to return at most
	 */
	List<DogZone> findNearest(String address, String city, int first) {
		Matcher street = STREET_AND_NUMBER.matcher(address.strip());
		if (!street.matches() || city.isBlank()) {
			return List.of();
		}
		return geocode(street.group(1), street.group(2), city.strip())
			.map((home) -> zonesAround(home).stream()
				.sorted(Comparator.comparingInt(DogZone::distanceInMetres))
				.limit(first)
				.toList())
			.orElse(List.of());
	}

	private Optional<LambertPoint> geocode(String street, String houseNumber, String city) {
		Map<String, String> address = new LinkedHashMap<>();
		address.put("Street", street);
		address.put("House", houseNumber);
		address.put("City", city);
		long started = System.nanoTime();
		Map<String, Element> result = properties(call(GEOCODER, this.geocoderUrl, geocodeAddress(address)));
		// an address the geocoder cannot find comes back with status "U" (unmatched) and
		// a point at NaN, with HTTP status 200
		if (!"M".equals(text(result.get("Status")))) {
			logger.info("SOAP GeocodeAddress at %s could not find %s %s, %s (%d ms)".formatted(this.geocoderUrl, street,
					houseNumber, city, millisSince(started)));
			return Optional.empty();
		}
		logger.info("SOAP GeocodeAddress at %s found %s with score %s (%d ms)".formatted(this.geocoderUrl,
				text(result.get("Match_addr")), text(result.get("Score")), millisSince(started)));
		Element shape = result.get("Shape");
		return Optional.of(new LambertPoint(coordinate(shape, "X"), coordinate(shape, "Y")));
	}

	private List<DogZone> zonesAround(LambertPoint home) {
		long started = System.nanoTime();
		List<Map<String, Element>> records = readRecords(call(MAP, this.mapUrl, queryFeatureData(home)));
		List<DogZone> zones = new ArrayList<>();
		for (Map<String, Element> record : records) {
			Element shape = record.get(SHAPE);
			LambertPoint zone = LambertPoint.fromDogZoneMap(coordinate(shape, "X"), coordinate(shape, "Y"));
			double distance = home.distanceTo(zone);
			// the map answers for a square, and the corners lie beyond the radius
			if (distance <= this.searchRadiusInMetres) {
				Map<String, String> fields = new HashMap<>();
				record.forEach((name, value) -> fields.put(name, SHAPE.equals(name) ? null : text(value)));
				zones.add(DogZone.fromRecord(fields, (int) Math.round(distance)));
			}
		}
		logger.info("SOAP QueryFeatureData at %s returned %d zones around the address, %d of them within %d m (%d ms)"
			.formatted(this.mapUrl, records.size(), zones.size(), this.searchRadiusInMetres, millisSince(started)));
		return zones;
	}

	private static long millisSince(long startedNanos) {
		return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
	}

	private Document call(String upstream, String url, Document payload) {
		DOMResult response = new DOMResult();
		try {
			this.soap.sendSourceAndReceiveToResult(url, new DOMSource(payload), response);
		}
		catch (WebServiceException ex) {
			// covers SOAP faults, timeouts and connection failures
			throw new UpstreamException(upstream, ex);
		}
		return (Document) response.getNode();
	}

	/**
	 * Builds the payload of a <code>GeocodeAddress</code> request. The address goes in as
	 * a property set, a list of keys and values.
	 */
	private static Document geocodeAddress(Map<String, String> address) {
		Document document = newDocument();
		Element request = root(document, GEOCODER_NAMESPACE, "GeocodeAddress");
		request.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xs", XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Element properties = propertyArray(typed(request, "Address", "e:PropertySet"));
		address.forEach((key, value) -> {
			Element property = typed(properties, "PropertySetProperty", "e:PropertySetProperty");
			appendText(property, "Key", key);
			appendText(property, "Value", value).setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:type",
					"xs:string");
		});
		// the geocoder answers in Lambert 72 unless a modifier asks for another system
		propertyArray(typed(request, "PropMods", "e:PropertySet"));
		return document;
	}

	/**
	 * Builds the payload of a <code>QueryFeatureData</code> request for the zones inside
	 * a square around a point. The square is given in Lambert 72, the system the map
	 * stores its zones in.
	 */
	private Document queryFeatureData(LambertPoint centre) {
		Document document = newDocument();
		Element request = root(document, MAP_NAMESPACE, "QueryFeatureData");
		appendText(request, "MapName", this.mapName);
		appendText(request, "LayerID", String.valueOf(this.layerId));
		Element filter = typed(request, "QueryFilter", "e:SpatialFilter");
		appendText(filter, "SubFields", FIELDS + "," + SHAPE);
		appendText(filter, "WhereClause", "");
		appendText(filter, "SearchOrder", "esriSearchOrderSpatial");
		appendText(filter, "SpatialRel", "esriSpatialRelIntersects");
		Element square = typed(filter, "FilterGeometry", "e:EnvelopeN");
		appendText(square, "XMin", String.valueOf(centre.x() - this.searchRadiusInMetres));
		appendText(square, "YMin", String.valueOf(centre.y() - this.searchRadiusInMetres));
		appendText(square, "XMax", String.valueOf(centre.x() + this.searchRadiusInMetres));
		appendText(square, "YMax", String.valueOf(centre.y() + this.searchRadiusInMetres));
		appendText(typed(square, "SpatialReference", "e:ProjectedCoordinateSystem"), "WKID",
				String.valueOf(LAMBERT_72));
		appendText(filter, "GeometryFieldName", SHAPE);
		return document;
	}

	/**
	 * Reads the property set of a geocoder answer into its values, keyed by name.
	 */
	private static Map<String, Element> properties(Document response) {
		Map<String, Element> properties = new HashMap<>();
		NodeList entries = response.getElementsByTagName("PropertySetProperty");
		for (int i = 0; i < entries.getLength(); i++) {
			List<Element> values = childElements(entries.item(i), "Value");
			if (!values.isEmpty()) {
				properties.put(childElements(entries.item(i), "Key").get(0).getTextContent(), values.get(0));
			}
		}
		return properties;
	}

	/**
	 * Reads the <code>RecordSet</code> of a map answer. It lists the fields first and
	 * then gives each record as values in the same order.
	 */
	private static List<Map<String, Element>> readRecords(Document response) {
		List<String> fieldNames = new ArrayList<>();
		NodeList fields = response.getElementsByTagName("Field");
		for (int i = 0; i < fields.getLength(); i++) {
			fieldNames.add(childElements(fields.item(i), "Name").get(0).getTextContent());
		}
		List<Map<String, Element>> records = new ArrayList<>();
		NodeList recordElements = response.getElementsByTagName("Record");
		for (int i = 0; i < recordElements.getLength(); i++) {
			List<Element> values = childElements(childElements(recordElements.item(i), "Values").get(0), "Value");
			if (values.size() != fieldNames.size()) {
				throw new UpstreamException(MAP,
						"a record has " + values.size() + " values for " + fieldNames.size() + " fields");
			}
			Map<String, Element> record = new LinkedHashMap<>();
			for (int field = 0; field < values.size(); field++) {
				record.put(fieldNames.get(field), values.get(field));
			}
			records.add(record);
		}
		return records;
	}

	private static double coordinate(Element point, String axis) {
		return Double.parseDouble(childElements(point, axis).get(0).getTextContent());
	}

	/**
	 * The source marks a missing value in two ways, with <code>xsi:nil</code> and with an
	 * empty string. Both become null.
	 */
	private static String text(Element value) {
		if (value == null) {
			return null;
		}
		String text = value.getTextContent().strip();
		return text.isEmpty() ? null : text;
	}

	private static Element root(Document document, String namespace, String operation) {
		Element request = document.createElementNS(namespace, "e:" + operation);
		// the xsi:type values below refer to the "e" prefix, so both prefixes are
		// declared
		// here explicitly
		request.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:e", namespace);
		request.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi",
				XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
		document.appendChild(request);
		return request;
	}

	private static Element propertyArray(Element propertySet) {
		return typed(propertySet, "PropertyArray", "e:ArrayOfPropertySetProperty");
	}

	private static Element typed(Element parent, String name, String type) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setAttributeNS(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi:type", type);
		parent.appendChild(child);
		return child;
	}

	private static List<Element> childElements(Node parent, String name) {
		List<Element> children = new ArrayList<>();
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child instanceof Element element && name.equals(element.getTagName())) {
				children.add(element);
			}
		}
		return children;
	}

	private static Element appendText(Element parent, String name, String text) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setTextContent(text);
		parent.appendChild(child);
		return child;
	}

	private static Document newDocument() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			return factory.newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
