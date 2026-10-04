# Public APIs we will integrate with

This catalogue lists the public APIs we can put behind the Petclinic GraphQL schema. Belgian government services come first, and EU-level services fill the gaps. Every API here is free. Where a service needs credentials, the registration is free as well.

Each entry says what the API does, how to call it, which operations are useful for a pet clinic, and which language the response data is in. Below, you can see sample responses of these services.

## Overview

| API | Owner | Protocol | Auth | Data language | Pet clinic link |
| --- | --- | --- | --- | --- | --- |
| [Antwerp dog zones](#antwerp-dog-zones) | City of Antwerp | SOAP and REST | Open | Dutch | Where an owner can walk a dog |
| [FAMHP medicines reference data](#famhp-medicines-reference-data) | Federal medicines agency | REST | Open | English, Dutch or French | Species codes for pet types |
| [RASFF pet food alerts](#rasff-pet-food-alerts) | EU, DG SANTE | REST | Open | English | Pet food safety |
| [EMA medicines file](#ema-medicines-file) | EU, EMA | REST | Open | English | Veterinary medicines |
| [EUR-Lex web service](#eur-lex-web-service) | EU Publications Office | SOAP | WS-Security | English and 23 others | Pet travel regulation |
| [Eurostat pet price index](#eurostat-pet-price-index) | EU, Eurostat | REST | Open | English | Cost of pets in Belgium |

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
| The same calls over SOAP | `GeocodeAddress` on the geocoder, `QueryFeatureData` with a `SpatialFilter` on the map | The geocoder's WSDL loads, the calls are untested |

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

## FAMHP medicines reference data

The federal medicines agency runs a public medicines database with a veterinary section. Its website loads reference data from `/api/resources`. That endpoint is the undocumented backend of the public site.

- **Endpoint:** `GET https://medicinesdatabase.be/api/resources`
- **Auth:** open for this endpoint. The product search (`/api/products`) returns `401` and is out of scope.
- **Language:** the `Accept-Language` header selects English, Dutch or French labels.

It returns 275 target species, plus delivery modes and authorisation types.

```json
[
  { "type": "targetSpecies", "code": "Ca", "label": "Dog" },
  { "type": "targetSpecies", "code": "Fe", "label": "Cat" },
  { "type": "targetSpecies", "code": "Lm", "label": "Rabbit" },
  { "type": "deliveryModus", "code": "", "label": "Medical prescription" }
]
```

## RASFF pet food alerts

RASFF is the EU's rapid alert system for food and feed. The public RASFF Window site loads its data from a JSON backend. That backend is undocumented and could change without notice.

- **Search:** `POST https://webgate.ec.europa.eu/rasff-window/backend/public/notification/search/consolidated/`
- **Detail:** `GET https://webgate.ec.europa.eu/rasff-window/backend/public/notification/view/id/{notifId}/`
- **Auth:** open.
- **Language:** English.

```json
{ "parameters": { "pageNumber": 1, "itemsPerPage": 1 }, "notificationReference": null, "subject": "pet food" }
```

The search for "pet food" returned 67 notifications. This is the detail of the most recent one:

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

## EMA medicines file

The European Medicines Agency publishes its medicines table as one JSON file, refreshed twice a day.

- **Endpoint:** `GET https://www.ema.europa.eu/en/documents/report/medicines-output-medicines_json-report_en.json`
- **Auth:** open.
- **Language:** English.

The file holds 2,746 medicines, of which 395 are veterinary. There is one operation: download the whole file.

The `species_veterinary` field is filled for 65 of the 395 veterinary records, and for only 2 of the 308 authorised ones. The file is therefore good for looking up a medicine by name or active substance, and poor for listing medicines by species.

## EUR-Lex web service

The EU's legal database offers a SOAP search service. Regulation (EU) No 576/2013 on the non-commercial movement of pet animals is the pet-related document.

- **Endpoint:** `https://eur-lex.europa.eu/EURLexWebService` (WSDL with `?wsdl`)
- **Auth:** WS-Security `UsernameToken`. Registration is free, and the credentials arrive by email.
- **Language:** English, and the other official EU languages through the search language parameter.

The WSDL declares one operation, `doQuery`, which runs an expert query. A call without credentials returns the fault `wsse:InvalidSecurity`. We have not called it with credentials.

## Eurostat pet price index

Eurostat's harmonised index of consumer prices has a category for "Pets and related products; veterinary and other services for pets".

- **Endpoint:** `GET https://ec.europa.eu/eurostat/api/dissemination/statistics/1.0/data/prc_hicp_midx?format=JSON&lang=EN&coicop=CP0934_0935&geo=BE&unit=I15&lastTimePeriod=3`
- **Auth:** open.
- **Language:** English.

```json
{
  "label": "HICP - monthly data (index) (1996-2025)",
  "dimension": { "time": { "category": { "index": { "2025-10": 0, "2025-11": 1, "2025-12": 2 } } } },
  "value": { "0": 132.91, "1": 133.49, "2": 133.47 }
}
```

The index uses 2015 as 100, so pet-related prices in Belgium were about 33% higher in December 2025.

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
