package be.vlaanderen.omgeving.capakeylodapi;

import be.vlaanderen.omgeving.capakeylodapi.service.PerceelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ViewResolver;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.jena.riot.Lang.JSONLD;

/**
 * https://geo.api.vlaanderen.be/capakey/v2/swagger/ui/index
 */
@Controller
public class HTMLController {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    List<ViewResolver> viewResolvers;

    @Autowired
    private PerceelService perceelService;

    @GetMapping(value = "id/perceel/{capakey1}/{capakey2}",
                produces = "text/html")
    public String getPerceelAsHtml(
            @PathVariable String capakey1,
            @PathVariable String capakey2) {
        return "redirect:/doc/perceel/{capakey1}/{capakey2}";
    }

    @GetMapping(value = "doc/perceel/{capakey1}/{capakey2}")
    public String getPerceelDoc(
            @PathVariable String capakey1,
            @PathVariable String capakey2,
            Model model) {

        String json = perceelService.getJson(capakey1, capakey2);
        String jsonld = rdfToLang(perceelService.extractModel(json, capakey1, capakey2), JSONLD);

        Map jsonAsMap;
        String polygon;
        Coordinate center;
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            jsonAsMap = objectMapper.convertValue(jsonNode, Map.class);
            polygon = getPolygonAsWkt(jsonNode);
            center = getCenterCoordinate(jsonNode);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        model.addAttribute("uri", "http://localhost:8080/id/perceel/" + capakey1 + "/" + capakey2);
        model.addAttribute("capakey", capakey1 + "/" + capakey2);
        model.addAttribute("polygon", polygon);
        model.addAttribute("centerX", center.getX());
        model.addAttribute("centerY", center.getY());
        model.addAttribute("fields", jsonAsMap);
        model.addAttribute("jsonld", jsonld);

        return "fiche";
    }

    private String getPolygonAsWkt(JsonNode jsonnode) throws JsonProcessingException {
        ArrayNode coords = (ArrayNode) objectMapper.readTree(jsonnode.get("geometry").get("shape").asText()).get("coordinates").get(0);

        List<String> wktCoords = new ArrayList<>();
        for (JsonNode coord : coords) {
            wktCoords.add(coord.get(0).asText() + " " + coord.get(1).asText());
        }
        String wktLambert = "POLYGON((" + String.join(", ", wktCoords) + "))";
        return convertToWkt(wktLambert);
    }

    private Coordinate getCenterCoordinate(JsonNode jsonnode) throws JsonProcessingException {
        ArrayNode coords = (ArrayNode) objectMapper.readTree(jsonnode.get("geometry").get("center").asText()).get("coordinates");
        String coordinaat1 = coords.get(0).asText();
        String coordinaat2 = coords.get(1).asText();
        String wktLabert = "POINT(" + coordinaat1 + " " + coordinaat2 + ")";
        return convertToCoordinate(wktLabert);
    }

    public String convertToWkt(String wktLamber) {
        try {
            WKTReader reader = new WKTReader();
            Geometry geom = reader.read(wktLamber);
            CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:31370");
            CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4326");
            MathTransform mathTransform = CRS.findMathTransform(sourceCrs, targetCrs, true);

            Geometry transform = JTS.transform(geom, mathTransform);
            WKTWriter wktWriter = new WKTWriter();
            return wktWriter.write(transform);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Coordinate convertToCoordinate(String wktLamber) {
        try {
            WKTReader reader = new WKTReader();
            Geometry geom = reader.read(wktLamber);
            CoordinateReferenceSystem sourceCrs = CRS.decode("EPSG:31370");
            CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:4326");
            MathTransform mathTransform = CRS.findMathTransform(sourceCrs, targetCrs, true);

            Geometry transform = JTS.transform(geom, mathTransform);
            return transform.getCoordinates()[0];
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String rdfToLang(org.apache.jena.rdf.model.Model model, Lang lang) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, lang);
        return writer.toString();
    }
}
