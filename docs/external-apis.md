# Public APIs we will integrate with

This catalogue lists the public APIs we can put behind the Petclinic GraphQL schema. Belgian government services come first, and EU-level services fill the gaps. Every API here is free. Where a service needs credentials, the registration is free as well.

Each entry says what the API does, how to call it, which operations are useful for a pet clinic, and which language the response data is in. Below, you can see sample responses of these services.

## Overview

| API | Owner | Protocol | Auth | Data language | Pet clinic link |
| --- | --- | --- | --- | --- | --- |
| [Antwerp dog zones](#antwerp-dog-zones) | City of Antwerp | SOAP and REST | Open | Dutch | Where an owner can walk a dog |
| [FAMHP medicines reference data](#famhp-medicines-reference-data) | Federal medicines agency | REST | Open | English, Dutch or French | Species codes for pet types |
| [RASFF pet food alerts](#rasff-pet-food-alerts) | EU, DG SANTE | REST | Open | English | Pet food safety |
| [EMA website reports](#ema-website-reports) | EU, EMA | REST | Open | English | Veterinary medicines and residue limits |
| [EUR-Lex web service](#eur-lex-web-service) | EU Publications Office | SOAP | WS-Security | English and 23 others | Pet travel regulation |
| [Eurostat pet price index](#eurostat-pet-price-index) | EU, Eurostat | REST | Open | English | Cost of pets in Belgium |
| [GBIF](#gbif) | GBIF, with Belgium as a participant | REST | Open | English | Sightings of dogs and cats, and the names of species |

## In the GraphQL schema

The schema reads every API in this catalogue except EUR-Lex, which needs credentials. The Antwerp dog zones are the field `Owner.nearestDogZones`. EMA, RASFF, FAMHP and Eurostat add 42 root fields to `Query`, with 55 types of their own, and GBIF adds 226 root fields with 177 types and 37 enums. Each field name starts with its API: `ema`, `rasff`, `famhp`, `eurostat` or `gbif`. The Petclinic types keep their fields, and every new field reads data.

Each of the four is an HTTP interface of Spring Framework (`@HttpExchange`), registered with `@ImportHttpServices` in a group of its own. Spring Boot reads the base URL and the timeouts of a group from `spring.http.serviceclient.<group>.*`. GBIF has too many operations for an interface method each, so a generated operation table drives one fetcher, as [the GBIF section](#gbif) explains. The `mock` profile points every base URL at the recorded responses in `src/main/resources/mock-upstreams`.

## Antwerp dog zones

The City of Antwerp publishes its off-leash dog zones (`hondenloopzones`) through ArcGIS Server. The server exposes the same layer over SOAP and over REST, so one data set can be shown through both protocols. The layer holds 97 zones.

- **SOAP endpoint:** `https://geodata.antwerpen.be/arcgissql/services/P_Portal/portal_publiek1/MapServer`
- **WSDL:** the same URL with `?wsdl`. It is 290 KB and declares 110 operations.
- **REST endpoint:** `https://geodata.antwerpen.be/arcgissql/rest/services/P_Portal/portal_publiek1/MapServer/9`
- **Auth:** open.
- **Language:** the WSDL is Esri's standard schema, so operation and type names are English. Field names and values are Dutch.

A second layer, `hondenloopzone_detail` (`portal_publiek6/MapServer/622`), holds the same 97 zones with inspection fields such as the number of complaints and the condition of the gates.

### Useful operations

| Operation | What it does | Verified |
| --- | --- | --- |
| `GetDefaultMapName` | Returns the map name that every other call needs (`portal_publiek1`) | Yes |
| `QueryFeatureCount` | Counts the zones matching a SQL `WhereClause` | Yes |
| `QueryFeatureData` | Returns the matching zones as a typed `RecordSet` | Yes |
| `QueryFeatureIDs`, `Find`, `Identify` | Other ways to look up features | Declared in the WSDL only |

The map name is `portal_publiek1` and the layer id is `9`.

### Sample SOAP request

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:e="http://www.esri.com/schemas/ArcGIS/3.5.0">
  <soap:Body>
    <e:QueryFeatureData>
      <MapName>portal_publiek1</MapName>
      <LayerID>9</LayerID>
      <QueryFilter xsi:type="e:QueryFilter">
        <SubFields>naam,straatnaam,postcode</SubFields>
        <WhereClause>postcode = '2000'</WhereClause>
      </QueryFilter>
    </e:QueryFeatureData>
  </soap:Body>
</soap:Envelope>
```

### Sample SOAP response

The response describes every field first and then lists the records as positional values.

```xml
<tns:QueryFeatureDataResponse>
  <Result xsi:type="tns:RecordSet">
    <Fields xsi:type="tns:Fields">
      <FieldArray xsi:type="tns:ArrayOfField">
        <Field xsi:type="tns:Field"><Name>OBJECTID</Name><Type>esriFieldTypeOID</Type>...</Field>
        <Field xsi:type="tns:Field"><Name>naam</Name><Type>esriFieldTypeString</Type>...</Field>
        ...
      </FieldArray>
    </Fields>
    <Records xsi:type="tns:ArrayOfRecord">
      <Record xsi:type="tns:Record">
        <Values xsi:type="tns:ArrayOfValue">
          <Value xsi:type="xsd:int">71</Value>
          <Value xsi:type="xsd:string">DE GERLACHELAAI</Value>
          <Value xsi:type="xsd:string">DE GERLACHEKAAI</Value>
          <Value xsi:type="xsd:string">2000</Value>
        </Values>
      </Record>
      ...
    </Records>
  </Result>
</tns:QueryFeatureDataResponse>
```

### The same query over REST

```
GET .../MapServer/9/query?where=netheid='goed'&outFields=*&returnGeometry=false&f=json
```

```json
{
  "attributes": {
    "id": "OPR0436",
    "naam": "KAREL COGGESTRAAT",
    "straatnaam": "KAREL COGGESTRAAT",
    "postcode": "2600",
    "district": "BERCHEM",
    "subtype": "hondenweide",
    "opp_ha": 0.55059925,
    "netheid": "goed",
    "verlichting": "onrechtstreeks",
    "hoogte_afsluiting": "2m00 en 1m00",
    "materiaal_afsluiting": "staalmat/ maasdraad",
    "aantal_vuilnisbakken": 1,
    "aantal_zitbanken": 2,
    "type_ondergrond": "gras"
  }
}
```

### From an address to the nearest dog zones

The dog zone layers do not take an address. The same server runs the city's geocoder, `LOC_CRAB`, which is built on the Flemish address register. The geocoder turns an address into a point, and a location query on a dog zone layer then finds the zones near that point.

- **Geocoder over REST:** `https://geodata.antwerpen.be/arcgissql/rest/services/LOC_CRAB/GeocodeServer/findAddressCandidates`
- **Geocoder over SOAP:** `https://geodata.antwerpen.be/arcgissql/services/LOC_CRAB/GeocodeServer`, with the WSDL at `?wsdl`
- **Address fields:** `Street`, `House`, `Postal` and `City`

Layer `9` stores each zone as a point, so it answers which zones lie within a distance. Layer `622` stores the outline of each zone as a polygon, so it answers whether a point lies inside a zone.

| Step | Call | Verified |
| --- | --- | --- |
| Address to coordinates | `findAddressCandidates` | Yes, over REST |
| Zones within a distance of a point | Layer `9`, `query` with `geometry`, `distance` and `units` | Yes, over REST |
| The zone that contains a point | Layer `622`, `query` with `geometry` and `spatialRel=esriSpatialRelIntersects` | Yes, over REST |
| The same flow over SOAP | `GeocodeAddress` on the geocoder, `QueryFeatureData` with a `SpatialFilter` on the map | Yes. `Owner.nearestDogZones` uses it |

```
GET .../LOC_CRAB/GeocodeServer/findAddressCandidates?Street=Groenendaallaan&House=394&Postal=2030&City=Antwerpen&outSR=4326&maxLocations=1&f=json
```

```json
{
  "spatialReference": { "wkid": 4326, "latestWkid": 4326 },
  "candidates": [
    {
      "address": "Groenendaallaan 394, 2030, Antwerpen",
      "location": { "x": 4.416367038546442, "y": 51.24592737192984 },
      "score": 100,
      "attributes": {}
    }
  ]
}
```

```
GET .../MapServer/9/query?geometry=4.416367,51.245927&geometryType=esriGeometryPoint&inSR=4326&spatialRel=esriSpatialRelIntersects&distance=2000&units=esriSRUnit_Meter&outFields=naam,straatnaam,postcode,district,subtype,opp_ha,netheid,verlichting&returnGeometry=true&outSR=4326&f=json
```

The query returned 15 zones. This is one of them:

```json
{
  "attributes": {
    "naam": "COLUMBIASTRAAT (TUSSEN DE LANGBLOKKEN)",
    "straatnaam": "COLUMBIASTRAAT",
    "postcode": "2030",
    "district": "ANTWERPEN",
    "subtype": "hondenweide",
    "opp_ha": 0.24345856,
    "netheid": "goed",
    "verlichting": "onrechtstreeks"
  },
  "geometry": { "x": 4.420750245365274, "y": 51.25417089772647 }
}
```

The nearest of the 15 is the dog meadow on Groenendaallaan in Merksem, about 400 m away. The same point sent to layer `622` does not fall inside any zone.

### The same flow over SOAP

`GeocodeAddress` takes the address as a property set, a list of keys and values:

```xml
<e:GeocodeAddress xmlns:e="http://www.esri.com/schemas/ArcGIS/10.8">
  <Address xsi:type="e:PropertySet">
    <PropertyArray xsi:type="e:ArrayOfPropertySetProperty">
      <PropertySetProperty xsi:type="e:PropertySetProperty"><Key>Street</Key><Value xsi:type="xs:string">Groenendaallaan</Value></PropertySetProperty>
      <PropertySetProperty xsi:type="e:PropertySetProperty"><Key>House</Key><Value xsi:type="xs:string">394</Value></PropertySetProperty>
      <PropertySetProperty xsi:type="e:PropertySetProperty"><Key>City</Key><Value xsi:type="xs:string">Antwerpen</Value></PropertySetProperty>
    </PropertyArray>
  </Address>
  <PropMods xsi:type="e:PropertySet"><PropertyArray xsi:type="e:ArrayOfPropertySetProperty"/></PropMods>
</e:GeocodeAddress>
```

The answer is a property set too. The point is in Belgian Lambert 72 (EPSG:31370), whose coordinates are metres, and it matches the Flanders geocoder to the centimetre:

```xml
<PropertySetProperty><Key>Shape</Key>
  <Value xsi:type="tns:PointN"><X>153324.41770908271</X><Y>215113.82966433687</Y>...<WKID>31370</WKID>...</Value>
</PropertySetProperty>
<PropertySetProperty><Key>Status</Key><Value xsi:type="xsd:string">M</Value></PropertySetProperty>
<PropertySetProperty><Key>Score</Key><Value xsi:type="xsd:double">100</Value></PropertySetProperty>
<PropertySetProperty><Key>Match_addr</Key><Value xsi:type="xsd:string">Groenendaallaan 394, 2030, Antwerpen</Value></PropertySetProperty>
```

`QueryFeatureData` then takes a `SpatialFilter` with a square around that point, in the same Lambert 72 metres:

```xml
<QueryFilter xsi:type="e:SpatialFilter">
  <SubFields>naam,postcode,district,netheid,verlichting,shape</SubFields>
  <WhereClause/>
  <SearchOrder>esriSearchOrderSpatial</SearchOrder>
  <SpatialRel>esriSpatialRelIntersects</SpatialRel>
  <FilterGeometry xsi:type="e:EnvelopeN">
    <XMin>151324.4</XMin><YMin>213113.8</YMin><XMax>155324.4</XMax><YMax>217113.8</YMax>
    <SpatialReference xsi:type="e:ProjectedCoordinateSystem"><WKID>31370</WKID></SpatialReference>
  </FilterGeometry>
  <GeometryFieldName>shape</GeometryFieldName>
</QueryFilter>
```

The map answers with the 17 zones of the square, each with its point in Web Mercator. Those points sit about 107 m north-west of where the REST interface puts the same zones. The map stores the zones in Lambert 72 and converts them to Web Mercator without the shift from the Belgian 1972 datum to WGS84: it reads the Belgian latitude and longitude of a zone as if they were WGS84. Taking the same steps backwards, from Web Mercator to latitude and longitude and from there to Lambert 72 on the Belgian ellipsoid, lands on the zone's correct position to the centimetre. After that correction, the distances over SOAP match the REST distances within 6 m: 418 m to Groenendaallaan, 967 m to Columbiastraat and 1001 m to Hendrik van Boutersemstraat.

### Dutch to English

| Field | English | Values seen |
| --- | --- | --- |
| `naam` | name | `DE GERLACHELAAI` |
| `straatnaam` | street name | `DE GERLACHEKAAI` |
| `postcode`, `district` | postal code, district | `2000`, `ANTWERPEN` |
| `subtype` | subtype | `hondenweide` (dog meadow) |
| `opp_ha` | area in hectares | `0.55059925` |
| `netheid` | cleanliness | `goed` (good), `aanvaardbaar` (acceptable), `slecht` (bad) |
| `verlichting` | lighting | `rechtstreeks` (direct), `onrechtstreeks` (indirect), `geen` (none) |
| `hoogte_afsluiting` | fence height | `2m00 en 1m00` (2 m and 1 m) |
| `materiaal_afsluiting` | fence material | `staalmat` (steel mesh), `maasdraad` (wire mesh) |
| `aantal_vuilnisbakken` | number of bins | `1` |
| `aantal_zitbanken` | number of benches | `2` |
| `type_ondergrond` | ground surface | `gras` (grass) |

### Things to know

- A wrong `MapName` returns a SOAP fault with HTTP status `200`. A client that only checks the status code treats the fault as a result.
- `netheid` also contains the misspelling `aanvaarbaar` and the free-text value `veel onkruid` (many weeds).
- Missing values appear both as `null` and as an empty string.
- A distance query returns the zones in the server's own order. The client computes the distance to each zone and sorts them.
- The geocoder's single-line field is called `Single Line Input`, with spaces. A request with `SingleLine` comes back with an empty candidate list, so the structured fields are the reliable way in.
- Over SOAP, the geocoder answers an address it cannot find with HTTP status `200`, `Status` `U` and a point at `NaN`. A client that skips the status reads `NaN` as a coordinate.
- Over SOAP, the map answers in Web Mercator whatever the request asks for. It ignores `OutputSpatialReference` in `QueryFeatureData` and in `QueryFeatureData2`, and `GeoTransformation` in `QueryFeatureData2`. The geocoder honours `OutputSpatialReference` in its `PropMods`.
- The geocoder and the map use different versions of Esri's schema: `ArcGIS/10.8` and `ArcGIS/3.5.0`.

## FAMHP medicines reference data

The federal medicines agency runs a public medicines database with a veterinary section. Its website loads reference data from `/api/resources`. That endpoint is the undocumented backend of the public site.

- **Endpoint:** `GET https://medicinesdatabase.be/api/resources`
- **Auth:** open for this endpoint. The product search (`/api/products`) returns `401` and is out of scope.
- **Language:** the `Accept-Language` header selects English, Dutch or French labels. Without the header, the labels come in Dutch. Petclinic asks for English.

One call returns 343 entries of five types in one list: 275 target species, 50 legal bases, 7 authorisation types, 6 document types and 5 delivery modes.

```json
[
  { "type": "targetSpecies", "code": "Ca", "label": "Dog" },
  { "type": "targetSpecies", "code": "Fe", "label": "Cat" },
  { "type": "targetSpecies", "code": "Lm", "label": "Rabbit" },
  { "type": "deliveryModus", "code": "", "label": "Medical prescription" }
]
```

The older species have short codes such as `Ca` for the dog. The species added later have their name as their code, for example `Dog (puppy)`.

### In the schema

Each field costs one call, and keeps the entries of one type.

| Root field | Returns |
| --- | --- |
| `famhpTargetSpecies(name)` | `[FamhpTargetSpecies!]!`, sorted by name |
| `famhpAuthorisationTypes` | `[FamhpAuthorisationType!]!`, sorted by name |
| `famhpLegalBases(usage)` | `[FamhpLegalBasis!]!`, sorted by name |
| `famhpDeliveryModes` | `[FamhpDeliveryMode!]!`, sorted by name |
| `famhpDocumentTypes` | `[FamhpDocumentType!]!`, in the database's own order |

## RASFF pet food alerts

RASFF is the EU's rapid alert system for food and feed. The public RASFF Window site loads its data from a JSON backend. That backend is undocumented and could change without notice.

- **Search:** `POST https://webgate.ec.europa.eu/rasff-window/backend/public/notification/search/consolidated/`
- **Detail:** `GET https://webgate.ec.europa.eu/rasff-window/backend/public/notification/view/id/{notifId}/`
- **Lists:** `GET https://webgate.ec.europa.eu/rasff-window/backend/public/{list}/list/`, for the ten lists that classify notifications and that the search filters take: `productCategory`, `productType`, `hazardCategory`, `riskDecision`, `notificationClassification`, `notificationBasis`, `actionTaken`, `notificationStatus`, `country` and `organization`
- **Auth:** open.
- **Language:** English.

```json
{ "parameters": { "pageNumber": 1, "itemsPerPage": 1 }, "notificationReference": null, "subject": "pet food" }
```

The search for "pet food" in the subject returned 67 notifications. The product category pet food (id `18429`) holds 270. This is the detail of the most recent one:

```json
{
  "id": 876211,
  "reference": "2026.8625",
  "subject": "Foreign body in pet food for cats from Germany",
  "notificationClassification": { "description": "information notification for follow-up" },
  "notificationBasis": { "description": "consumer complaint" },
  "product": {
    "description": "cat food",
    "productCategory": { "description": "pet food" },
    "hazards": [{ "name": "foreign body  - foreign bodies", "analyticalResult": "0,5-1", "unit": "cm" }]
  },
  "risk": { "riskDecision": "potential risk" }
}
```

### Things to know

- The search numbers its pages from 1. Page 0 comes back with the right totals and an empty list.
- The detail of an id that does not exist, or that the public may not see, comes back as `401 Unauthorized` instead of `404 Not Found`.
- Each list arrives in an object with a single key, and the key differs from list to list: `hazardCategory/list/` answers with `hazardCategories`, `productType/list/` with `notificationTypes` and `notificationStatus/list/` with `response`.
- Subjects end with spaces, and hazard names contain double spaces.
- A detail also names the officials who handled the notification. The schema leaves their names out.
- For pet food, `numberOfPersonsAffected` counts animals. One notification reports 2, with the illness "One dog has died, the other dog has been admitted to a veterinary clinic".

### In the schema

Each field costs one call. The search pages on the RASFF side, so a page of `rasffNotifications` is a page of RASFF.

| Root field | Returns |
| --- | --- |
| `rasffNotifications(filter, page, size)` | `RasffNotificationPage!`, the most recently validated first |
| `rasffNotification(id)` | `RasffNotification`, with the product, hazards, risk, countries, measures and follow-ups |
| `rasffProductCategories`, `rasffProductTypes`, `rasffHazardCategories`, `rasffRiskDecisions`, `rasffNotificationClassifications`, `rasffNotificationBases`, `rasffActionsTaken`, `rasffNotificationStatuses` | `[RasffTerm!]!`, the ids that the filter takes |
| `rasffCountries` | `[RasffCountry!]!` |
| `rasffNetworkMembers` | `[RasffMember!]!`, the members that send notifications |

## EMA website reports

The European Medicines Agency publishes 16 tables of its website as JSON reports, refreshed twice a day. A report is one file with every record of its table, and the only operation is to download the whole file.

- **Endpoint:** `GET https://www.ema.europa.eu/en/documents/report/{report}.json`
- **List of reports:** [Download website data in JSON data format](https://www.ema.europa.eu/en/about-us/about-website/download-website-data-json-data-format)
- **Auth:** open.
- **Language:** English. The EPAR documents also link to their translations.

| Report | Records | Size | Root field |
| --- | --- | --- | --- |
| `medicines-output-medicines_json-report_en` | 2,746 | 6.6 MB | `emaMedicines` |
| `documents-output-epar_documents_json-report_en` | 20,241 | 28 MB | `emaEparDocuments` |
| `documents-output-non_epar_documents_json-report_en` | 50,426 | 34 MB | `emaDocuments` |
| `events-json-report_en` | 2,501 | 1.4 MB | `emaEvents` |
| `general-json-report_en` | 1,065 | 0.6 MB | `emaWebsiteArticles` |
| `medicine-use-outside-eu-output-json-report_en` | 19 | 25 KB | `emaOutsideEuOpinions` |
| `medicines-output-herbal_medicines-report-output-json_en` | 251 | 0.2 MB | `emaHerbalSubstances` |
| `medicines-output-maximum_residue_limits-json-report_en` | 529 | 0.3 MB | `emaMaximumResidueLimits` |
| `medicines-output-orphan_designations-json-report_en` | 3,310 | 2 MB | `emaOrphanDesignations` |
| `medicines-output-paediatric_investigation_plans-output-json-report_en` | 3,377 | 3.8 MB | `emaPaediatricInvestigationPlans` |
| `medicines-output-periodic_safety_update_report_single_assessments-output-json-report_en` | 2,720 | 1.4 MB | `emaPeriodicSafetyAssessments` |
| `medicines-output-post_authorisation_json-report_en` | 152 | 0.2 MB | `emaPostAuthorisationProcedures` |
| `news-json-report_en` | 3,890 | 2.2 MB | `emaNews` |
| `referrals-output-json-report_en` | 592 | 0.7 MB | `emaReferrals` |
| `shortages-output-json-report_en` | 85 | 69 KB | `emaShortages` |
| `dhpc-output-json-report_en` | 174 | 0.1 MB | `emaSafetyCommunications` |

The medicines report holds 395 veterinary medicines. The `species_veterinary` field is filled for 65 of them, and for only 2 of the 308 authorised ones. The report is therefore good for looking up a medicine by name or active substance, and poor for listing medicines by species. The maximum residue limits report is veterinary throughout: it says how much of a substance may remain in food from treated animals.

### Things to know

- Every value is a text. Most reports write dates as `04/10/2026`, the two document reports write ISO-8601 timestamps, yes-or-no columns hold `Yes` and `No`, and columns with several values separate them with semicolons, sometimes with a value twice (`Veterinary;Veterinary`).
- The herbal report's `meta.total_records` says 204, and the file holds 251 records.
- Spring's JDK HTTP client applies the read timeout to the whole exchange, the body included. The EMA client has a read timeout of 60 seconds, because the 10 seconds of the other services would cut a slow download of the 34 MB report.

### In the schema

Each root field reads one report, filters its records in memory and returns one page, the most recently changed first. Petclinic downloads a report the first time a query needs it and keeps it for 12 hours (`petclinic.upstream.ema.refresh-after`). The two document reports take about 5 seconds to download and read, and all 16 reports together take about 120 MB of heap.

## EUR-Lex web service

The EU's legal database offers a SOAP search service. Regulation (EU) No 576/2013 on the non-commercial movement of pet animals is the pet-related document.

- **Endpoint:** `https://eur-lex.europa.eu/EURLexWebService` (WSDL with `?wsdl`)
- **Auth:** WS-Security `UsernameToken`. Registration is free, and the credentials arrive by email.
- **Language:** English, and the other official EU languages through the search language parameter.

The WSDL declares one operation, `doQuery`, which runs an expert query. A call without credentials returns the fault `wsse:InvalidSecurity`. We have not called it with credentials.

## Eurostat pet price index

Eurostat's harmonised index of consumer prices (HICP) has categories for pets, pet products and veterinary services. In 2026 Eurostat moved the index to version 2 of the European classification of consumption (ECOICOP 2), with new datasets and new category codes. The datasets of version 1, such as `prc_hicp_midx`, end with December 2025.

- **Endpoint:** `GET https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_minr?format=JSON&lang=EN&coicop18=CP0932&geo=BE&unit=I25&lastTimePeriod=3`
- **Auth:** open.
- **Language:** English.

| Code | Category |
| --- | --- |
| `CP0932` | Pets and pet products |
| `CP09321` | Pets |
| `CP09322` | Products for pets and other household animals |
| `CP0945` | Veterinary and other services for pets |

```json
{
  "label": "Harmonised index of consumer prices (HICP) - ECOICOP ver.2 - indices and rates of change, monthly data",
  "dimension": { "time": { "category": { "index": { "2026-07": 0, "2026-08": 1, "2026-09": 2 } } } },
  "value": { "0": 99.8, "1": 98.04 }
}
```

The index uses 2025 as 100. The latest month appears in the time dimension before Eurostat publishes its value, as September 2026 does here.

### Things to know

- The values are numbered in one sequence across all the dimensions. Petclinic filters every dimension except time to one value, so each number is a position in time.
- A filter value the dataset does not know, such as the country code `XX`, comes back with status `200` and an empty dimension.
- The contributions to the inflation of the euro area (`prc_hicp_ctr`) exist for the euro area only, and the pet categories round to 0.0 percentage points.

### In the schema

Each field costs one call and returns an `EurostatSeries` for one category (`PETS_AND_PET_PRODUCTS` by default) and one country (`BE` by default). Without `from` and `to`, a monthly series covers the latest 12 months and a yearly one the latest 5 years.

| Root field | Dataset and unit |
| --- | --- |
| `eurostatPetPriceIndex` | `prc_hicp_minr`, index with 2025 as 100 |
| `eurostatPetPriceMonthlyChange` | `prc_hicp_minr`, change from the month before |
| `eurostatPetPriceAnnualChange` | `prc_hicp_minr`, change from the same month a year before |
| `eurostatPetPriceMovingAverageChange` | `prc_hicp_minr`, change of the 12-month average |
| `eurostatPetPriceIndexAtConstantTaxRates` | `prc_hicp_ct`, index with 2025 as 100 at constant tax rates |
| `eurostatPetPriceContributionToEuroAreaInflation` | `prc_hicp_ctr`, percentage points of the euro area's inflation |
| `eurostatPetPriceAnnualAverageIndex` | `prc_hicp_ainr`, yearly average index |
| `eurostatPetPriceAnnualAverageChange` | `prc_hicp_ainr`, change of the yearly average |
| `eurostatPetSpendingWeight` | `prc_hicp_iw`, share of household spending in parts per thousand |

## GBIF

GBIF, the Global Biodiversity Information Facility, is an international network that governments fund, with its secretariat in Copenhagen. Belgium takes part through the Belgian Biodiversity Platform. The API publishes where species live: the names and the classification of species, more than 3 billion occurrences (records of an organism at a place and a time, such as a sighting of a dog), the datasets they come from, the organizations that publish them, and the scientific collections of museums and herbaria.

- **Endpoint:** `GET https://api.gbif.org/v1/occurrence/search?taxonKey=6164210&country=BE&limit=2`
- **OpenAPI documents:** [GBIF API Reference](https://techdocs.gbif.org/en/openapi/), five of them: species, occurrence, registry, vocabulary and literature.
- **Auth:** open for reading. Downloads and the data of a user need a GBIF account.
- **Language:** English. The common names of species come in many languages.

In the GBIF backbone taxonomy, the dog is `Canis lupus familiaris` with the key `6164210`.

```json
{
  "offset": 0, "limit": 2, "endOfRecords": false, "count": 3749,
  "results": [
    { "key": 5937753054, "scientificName": "Canis familiaris Linnaeus, 1758", "countryCode": "BE", "eventDate": "2026-01-02" }
  ]
}
```

### Things to know

- The documents describe 282 read operations. 226 of them answer JSON without a login, and each one became a root field. The others answer files such as ZIP and XML, need a GBIF account (`401` or `403`), or have the problems below.
- The verbatim answers name each Darwin Core term by its full URI, such as the URI of `scientificName` in the namespace `rs.tdwg.org/dwc/terms/`. A URI cannot be a GraphQL name, so the three verbatim operations are left out.
- The GeoJSON answers carry untyped geometries, so their features would lose their coordinates. Both GeoJSON operations are left out.
- The possible duplicates of collections and institutions answer hundreds of groups in one piece, 581 KB for the collections. They are tools for GBIF's editors and are left out.
- The experimental multimedia operations answer `404` for every species, and `listForInstitution` did not answer within 60 seconds.
- The answers differ from the documents in a few places, which the generator corrects: the tags of an entity come as a list, `term` is a path variable, and the field `term` of a download column is a string.
- An empty answer comes as `204 No Content`, for example the IUCN category of a taxon that IUCN has not assessed. The citation of a download is plain text under the content type `application/json`.
- Counts and keys pass the 2,147,483,647 of `Int`: GBIF counts more than 3 billion occurrences.
- GBIF leaves the email address and the account of whoever suggested a change to a collection empty for anonymous readers.

### In the schema

GBIF adds 226 root fields, 177 types and 37 enums. Every name starts with `gbif` or `Gbif`, and the fields follow the order of the five documents: species, occurrences, registry, vocabularies and literature.

A script generated the fields and their types from the OpenAPI documents, together with `src/main/resources/gbif/operations.json`, which holds one line for each operation: its path, and where each argument goes. With more than 200 operations, one `RestOperationFetcher` serves every field instead of an HTTP interface method for each. It builds the URL from the arguments, calls GBIF, logs the call, and returns the JSON as GBIF sent it:

```
REST GET https://api.gbif.org/v1/occurrence/search?taxonKey=6164210&country=BE&offset=0&limit=2 returned 2 of 3,749 results (183 ms)
```

- **Paging:** the fields take `page` and `size`, from 1 to 50, like the rest of the schema, and the fetcher turns them into GBIF's `offset` and `limit`.
- **Errors:** `404` and `204` become null. A `400` becomes a `BAD_REQUEST` error that repeats GBIF's reason, for example "At least one param to check the same field is required".
- **Maps:** GBIF answers some counts as an object whose keys are data, such as `countByKingdom`. The schema has a list of entries with a `key` and a `value` for each of them.
- **Descriptions:** most come from GBIF's documents, in GBIF's wording. Where a document leaves one out or is terse, such as "Name usage key.", the generator writes one, so every root field, argument, type, field and enum has a description.
- **Size:** the GBIF part is about 93,500 of the schema's 111,000 tokens, counted with OpenAI's `o200k_base` tokenizer. GATool now ranks 2,351 schema coordinates, up from 480. Building its embedding index the first time took about 54 seconds, and a later start, which reads the index from its cache, took 2.9 seconds.
- **Mock mode:** `mock-upstreams/__files/gbif` holds one recorded answer for each operation, and `mappings/gbif.json` serves it for any arguments. The recordings of GBIF, EMA and RASFF have their personal email addresses and phone numbers replaced, and the contact persons of RASFF notifications as well.

## Checked and left out

| API | Reason |
| --- | --- |
| KBO Public Search (SOAP, business register) | Paid. The registration page announces that it becomes free by the end of 2026. |
| BeSt address (BOSA) | OAuth2 credentials are described for public services only. |
| FAMHP product search | Returns `401`. It is a private backend. |
| Sanitel-Med (SOAP) | Returns `401`, and it covers food-producing animals only. |
| eHealth platform (SOAP) | Returns `403` on the WSDL. Access needs certificates. |
| DogID and CatID | Web applications behind reCAPTCHA. We did not find a documented API. |
| Flanders approved breeders and shelters | Published as PDF files. |
| Orders of veterinarians | Public websites. We did not find an API. |
