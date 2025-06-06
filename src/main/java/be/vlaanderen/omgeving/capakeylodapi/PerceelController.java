package be.vlaanderen.omgeving.capakeylodapi;

import be.vlaanderen.omgeving.capakeylodapi.configuration.JsonldConfiguration;
import be.vlaanderen.omgeving.capakeylodapi.configuration.ReasoningModelConfiguration;
import be.vlaanderen.omgeving.capakeylodapi.reasoner.Reasoner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private JsonldConfiguration jsonldConfiguration ;
    @Autowired
    private ReasoningModelConfiguration reasoningModelConfiguration ;

    @GetMapping("/{capakey1}/{capakey2}")
    public ResponseEntity<String> getPerceel(
            @PathVariable String capakey1,
            @PathVariable String capakey2,
            @RequestHeader("Accept") String accept) {

        String url = String.format("https://geo.api.vlaanderen.be/capakey/v2/parcel/%s/%s?geometry=full", capakey1, capakey2);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // TODO Server side error 500 ; Method threw 'org.springframework.web.client.HttpServerErrorException$InternalServerError' exception.
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        String json = response.getBody();

        if (accept.contains("application/json")) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        }

        // Transform JSON -> JSON-LD
        if (accept.contains("application/ld+json")) {
            String jsonld = rdfToJsonLd(jsonLdToRdf(transformToJsonLd(json, capakey1 + "/" + capakey2)));
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/ld+json"))
                    .body(jsonld);
        }

        // Transform JSON -> Turtle
        if (accept.contains("text/turtle")||accept.contains("text/html")) {
            String turtle = rdfToTurtle(jsonLdToRdf(transformToJsonLd(json, capakey1 + "/" + capakey2)));
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("text/turtle"))
                    .body(turtle);
        }

        // Transform JSON -> RDF/XML
        if (accept.contains("application/rdf+xml")) {
            String rdfXml = rdfToRdfXml(jsonLdToRdf(transformToJsonLd(json, capakey1 + "/" + capakey2)));
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/rdf+xml"))
                    .body(rdfXml);
        }

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Unsupported Accept header");
    }

    private String transformToJsonLd(String json, String fullCapakey) {
        // Deze methode verwerkt de JSON en maakt een JSON-LD string.
        // In een productie-omgeving zou je dit netjes modelleren. Voor nu volstaat een snelle parse & build.
        try {
            ObjectMapper mapper = new ObjectMapper();
//            JsonNode
            ObjectNode root = (ObjectNode) mapper.readTree(json);
            JsonNode context = jsonldConfiguration.getJsonLDContext();
            ArrayNode coords = (ArrayNode) ((ObjectNode) mapper.readTree(root.get("geometry").get("shape").asText())).get("coordinates").get(0);
            List<String> wktCoords = new ArrayList<>();
            for (JsonNode coord : coords) {
                wktCoords.add(coord.get(0).asText() + " " + coord.get(1).asText());
            }
            String wkt = "POLYGON((" + String.join(", ", wktCoords) + "))";

            ObjectNode jsonld = mapper.createObjectNode();
            jsonld.set("@context", context);
            jsonld.put("@id", "https://data.vlaanderen.be/id/perceel/" + fullCapakey);
            //jsonld.put("type", "geo:Feature");
            jsonld.put("departmentCode", root.get("departmentCode").asText());
            jsonld.put("departmentName", root.get("departmentName").asText());
            jsonld.put("sectionCode", root.get("sectionCode").asText());
            jsonld.put("perceelnummer", root.get("perceelnummer").asText());
            jsonld.put("grondnummer", root.get("grondnummer").asText());
            jsonld.put("exponent", root.get("exponent").asText());
            jsonld.put("macht", root.get("macht").asText());
            jsonld.put("bisnummer", root.get("bisnummer").asText());

            ObjectNode geometry = mapper.createObjectNode();
            geometry.put("@id", "https://data.vlaanderen.be/id/geometry/capakey/" + fullCapakey);
            //geometry.put("type", "sf:Polygon");
            //geometry.put("type", "geo:Geometry");
            geometry.put("wkt", wkt);
            jsonld.set("geometry", geometry);

            ObjectNode address = mapper.createObjectNode();
            //address.put("type", "locn:Address");
            address.set("fullAddress", root.get("adres"));
            address.set("postName", root.get("municipalityName"));
            jsonld.set("adres", address);

            ObjectNode locn = mapper.createObjectNode();
            //locn.put("type", "dct:Location");
            locn.set("municipalityName", root.get("municipalityName"));
            jsonld.set("location", locn);

            ObjectNode ident = mapper.createObjectNode();
            ident.put("@id", "https://data.vlaanderen.be/id/identifier/capakey/" + fullCapakey);
            //ident.put("type", "adms:Identifier");
            ident.put("capakey", fullCapakey);
            jsonld.set("identifier", ident);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonld);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to transform JSON to JSON-LD", e);
        }
    }

    @NotNull
    private Model jsonLdToRdf(String jsonld) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream is = new ByteArrayInputStream(jsonld.getBytes(StandardCharsets.UTF_8))) {
            RDFParser.source(is).lang(Lang.JSONLD).parse(model);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON-LD to RDF", e);
        }
        return new Reasoner().inferTriples(model, reasoningModelConfiguration.loadTurtleFromClasspath(), reasoningModelConfiguration.getRules());
    }

    private String rdfToTurtle(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, Lang.TURTLE);
        return writer.toString();
    }

    private String rdfToRdfXml(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, Lang.RDFXML);
        return writer.toString();
    }

    private String rdfToJsonLd(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, Lang.JSONLD);
        return writer.toString();
    }
}