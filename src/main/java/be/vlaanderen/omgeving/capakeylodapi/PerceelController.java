package be.vlaanderen.omgeving.capakeylodapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/id/perceel")
public class PerceelController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{capakey1}/{capakey2}")
    public ResponseEntity<String> getPerceel(
            @PathVariable String capakey1,
            @PathVariable String capakey2,
            @RequestHeader("Accept") String accept) {

        String url = String.format("https://geo.api.vlaanderen.be/capakey/v2/parcel/%s/%s?geometry=full", capakey1, capakey2);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String json = response.getBody();

        if (accept.contains("application/json")) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        }

        // Transform JSON -> JSON-LD
        if (accept.contains("application/ld+json")) {
            String jsonld = transformToJsonLd(json, capakey1 + "/" + capakey2);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/ld+json"))
                    .body(jsonld);
        }

        // Transform JSON -> Turtle
        if (accept.contains("text/turtle")) {
            String jsonld = transformToJsonLd(json, capakey1 + "/" + capakey2);
            String turtle = jsonLdToTurtle(jsonld);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/turtle"))
                    .body(turtle);
        }

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Unsupported Accept header");
    }

    private String transformToJsonLd(String json, String fullCapakey) {
        // Deze methode verwerkt de JSON en maakt een JSON-LD string zoals in jouw voorbeeld.
        // In een productie-omgeving zou je dit netjes modelleren. Voor nu volstaat een snelle parse & build.
        try {

            ObjectMapper mapper = new ObjectMapper();
//            JsonNode
            ObjectNode root = (ObjectNode) mapper.readTree(json);

            ObjectNode context = mapper.createObjectNode();
            context.put("geo", "http://www.opengis.net/ont/geosparql#");
            context.put("sf", "http://www.opengis.net/ont/sf#");
            context.put("locn", "http://www.w3.org/ns/locn#");
            context.put("capakey", "https://data.vlaanderen.be/id/perceel/");
            context.put("perc", "https://data.vlaanderen.be/ns/perceel#");
            context.put("xs", "http://www.w3.org/2001/XMLSchema#");
            context.put("dct", "http://purl.org/dc/terms/");
            context.put("adms", "https://www.w3.org/ns/adms#");


            ObjectNode adres = mapper.createObjectNode();
            adres.put("@id", "locn:address");
            adres.put("@container", "@set");
            adres.put("@type", "@id");
            context.set("adres", adres);

            ObjectNode fullAddress = mapper.createObjectNode();
            fullAddress.put("@id", "locn:fullAddress");
            context.set("fullAddress", fullAddress);



            ObjectNode ident = mapper.createObjectNode();
            ident.put("@id", "adms:identifier");
            ident.put("@type", "@id");
            context.set("identifier", ident);

            ObjectNode location = mapper.createObjectNode();
            location.put("@id", "locn:location");
            location.put("@type", "@id");
            context.set("location", location);

            ObjectNode municipality = mapper.createObjectNode();
            municipality.put("@id", "locn:geographicName");
            context.set("municipalityName", municipality);

            ObjectNode pn = mapper.createObjectNode();
            pn.put("@id", "locn:postName");
            context.set("postName", pn);

            context.set("geometry", mapper.createObjectNode().put("@id", "geo:hasGeometry"));

            context.set("wkt", mapper.createObjectNode()
                    .put("@id", "geo:asWKT")
                    .put("@type", "geo:wktLiteral"));
            context.put("type", "@type");

            // Geometry in WKT
            ArrayNode coords = (ArrayNode) ((ObjectNode) mapper.readTree(root.get("geometry").get("shape").asText())).get("coordinates").get(0);
            List<String> wktCoords = new ArrayList<>();
            for (JsonNode coord : coords) {
                wktCoords.add(coord.get(0).asText() + " " + coord.get(1).asText());
            }
            String wkt = "POLYGON((" + String.join(", ", wktCoords) + "))";

            ObjectNode jsonld = mapper.createObjectNode();
            jsonld.set("@context", context);
            jsonld.put("@id", "https://data.vlaanderen.be/id/perceel/" + fullCapakey);
            jsonld.put("capakey", fullCapakey);
            //jsonld.put("municipalityName", root.get("municipalityName").asText());
            jsonld.put("departmentName", root.get("departmentName").asText());
            jsonld.put("sectionCode", root.get("sectionCode").asText());
            jsonld.put("grondnummer", root.get("grondnummer").asText());
            jsonld.put("exponent", root.get("exponent").asText());
            jsonld.put("bisnummer", root.get("bisnummer").asText());
            //jsonld.set("adres", root.get("adres"));

            ObjectNode geometry = mapper.createObjectNode();
            geometry.put("type", "sf:Polygon");
            geometry.put("wkt", wkt);
            jsonld.set("geometry", geometry);

            ObjectNode address = mapper.createObjectNode();
            address.put("type", "locn:Address");
            address.set("fullAddress", root.get("adres"));
            address.set("postName", root.get("municipalityName"));
            jsonld.set("adres", address);

            ObjectNode locn = mapper.createObjectNode();
            locn.put("type", "dct:Location");
            locn.set("municipalityName", root.get("municipalityName"));
            jsonld.set("location", locn);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonld);

        }
        catch (Exception e) {
            throw new RuntimeException("Failed to transform JSON to JSON-LD", e);
        }
    }

    private String jsonLdToTurtle(String jsonld) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream is = new ByteArrayInputStream(jsonld.getBytes(StandardCharsets.UTF_8))) {
            RDFParser.source(is).lang(Lang.JSONLD).parse(model);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON-LD to RDF", e);
        }

        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, Lang.TURTLE);
        return writer.toString();
    }
}
