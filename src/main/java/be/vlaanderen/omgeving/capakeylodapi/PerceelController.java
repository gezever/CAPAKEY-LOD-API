package be.vlaanderen.omgeving.capakeylodapi;

import be.vlaanderen.omgeving.capakeylodapi.service.PerceelService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.StringWriter;

@RestController
@RequestMapping("/id/perceel")
@CrossOrigin
public class PerceelController {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private PerceelService perceelService;

    @GetMapping(value = "/{capakey1}/{capakey2}",
                produces = "application/json")
    public ResponseEntity<String> getPerceelAsJson(
            @PathVariable String capakey1,
            @PathVariable String capakey2) {

        String json = perceelService.getJson(capakey1, capakey2);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

    @GetMapping(value = "/{capakey1}/{capakey2}",
                produces = "application/ld+json")
    public ResponseEntity<String> getPerceelAsJsonLd(
            @PathVariable String capakey1,
            @PathVariable String capakey2) {

        String jsonld = perceelService.getJsonLd(capakey1, capakey2);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/ld+json"))
                .body(jsonld);
    }

    @GetMapping(value = "/{capakey1}/{capakey2}",
                produces = "text/turtle")
    public ResponseEntity<String> getPerceelAsTurtle(
            @PathVariable String capakey1,
            @PathVariable String capakey2) {

        Model model = perceelService.extractModel(capakey1, capakey2);
        String turtle = rdfToLang(model, Lang.TURTLE);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("text/turtle"))
                .body(turtle);
    }

    @GetMapping(value = "/{capakey1}/{capakey2}",
                produces = "application/rdf+xml")
    public ResponseEntity<String> getPerceelAsRdf(
            @PathVariable String capakey1,
            @PathVariable String capakey2) {

        Model model = perceelService.extractModel(capakey1, capakey2);
        String rdfxml = rdfToLang(model, Lang.RDFXML);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/rdf+xml"))
                .body(rdfxml);
    }

    private String rdfToLang(Model model, Lang lang) {
        StringWriter writer = new StringWriter();
        RDFDataMgr.write(writer, model, lang);
        return writer.toString();
    }


}