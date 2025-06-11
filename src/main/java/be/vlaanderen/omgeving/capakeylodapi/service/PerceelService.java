package be.vlaanderen.omgeving.capakeylodapi.service;

import be.vlaanderen.omgeving.capakeylodapi.configuration.JsonldConfiguration;
import be.vlaanderen.omgeving.capakeylodapi.configuration.ReasoningModelConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Service
public class PerceelService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private JsonldConfiguration jsonldConfiguration;

    @Autowired
    private ReasoningModelConfiguration reasoningModelConfiguration;

    public String getJson(String capakey1, String capakey2) {
        return extractOriginalJson(capakey1, capakey2);
    }

    public String getJsonLd(String capakey1, String capakey2) {
        Model model = extractModel(capakey1, capakey2);
        return rdfToJsonLd(model);
    }

    public String getJsonLd(String originalJson, String capakey1, String capakey2) {
        Model model = extractModel(originalJson, capakey1, capakey2);
        return rdfToJsonLd(model);
    }

    public Model extractModel(String capakey1, String capakey2) {
        String json = extractJsonLd(capakey1, capakey2);
        return parseModelFromJsonLD(json);
    }

    public Model extractModel(String originalJson, String capakey1, String capakey2) {
        String jsonld = transformToJsonLd(originalJson, capakey1 + "/" + capakey2);
        return parseModelFromJsonLD(jsonld);
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
            String wkt = "SRID=31370;POLYGON((" + String.join(", ", wktCoords) + "))";

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

    private String extractJsonLd(String capakey1, String capakey2) {
        String json = extractOriginalJson(capakey1, capakey2);
        return transformToJsonLd(json, capakey1 + "/" + capakey2);
    }

    private String extractOriginalJson(String capakey1, String capakey2) {
        String url = String.format("https://geo.api.vlaanderen.be/capakey/v2/parcel/%s/%s?geometry=full", capakey1, capakey2);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        return response.getBody();
    }

    private String rdfToJsonLd(Model model) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, Lang.JSONLD);
        return frameJsonLd(writer.toString());
    }

    private String frameJsonLd(String jsonldString) {
        try {
            Object jsonObject = JsonUtils.fromString(jsonldString);
            // Define your frame
            Object frame = jsonldConfiguration.getJsonLDFrame();
            // Frame the JSON-LD
            JsonLdOptions options = new JsonLdOptions();
            Map<String, Object> framed = JsonLdProcessor.frame(jsonObject, frame, options);
            if (framed.containsKey("@graph")) {
                List<?> graph = (List<?>) framed.get("@graph");
                if (graph.size() == 1) {
                    // Promote the node outside of @graph
                    Map<String, Object> singleNode = (Map<String, Object>) graph.get(0);
                    singleNode.put("@context", framed.get("@context"));
                    framed = singleNode;
                }
            }
            return JsonUtils.toPrettyString(framed);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Model parseModelFromJsonLD(String jsonld) {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream is = new ByteArrayInputStream(jsonld.getBytes(StandardCharsets.UTF_8))) {
            RDFParser.source(is).lang(Lang.JSONLD).parse(model);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON-LD to RDF", e);
        }
        return inferTriples(model, reasoningModelConfiguration.loadTurtleFromClasspath(), reasoningModelConfiguration.getRules());
    }

    public Model inferTriples(Model dataModel, Model ontologyModel, List<Rule> rules) {
        // Combine both models
        Model combinedModel = ModelFactory.createUnion(ontologyModel, dataModel);
        // Apply a reasoner to the combined model
        // Alternative reasoners
        // Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        // Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        Reasoner reasoner = new GenericRuleReasoner(rules);
        reasoner.setDerivationLogging(true);  // optional

        InfModel infModel = ModelFactory.createInfModel(reasoner, combinedModel);
        return infModel.getDeductionsModel().union(dataModel);
    }
}
