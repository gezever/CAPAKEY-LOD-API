package be.vlaanderen.omgeving.capakeylodapi.reasoner;


import java.io.BufferedReader;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.apache.jena.reasoner.rulesys.*;
import java.io.InputStreamReader;
import java.util.List;

import java.io.InputStream;

public class OntologyReasoning {

    @Value("classpath:geosparql_vocab_all.ttl")
    private Resource geosparql;

    @Value("classpath:locn.ttl")
    private Resource locn;

    public static Model loadTurtleFromClasspath() {
        Model model_adms = ModelFactory.createDefaultModel();
        InputStream adms = OntologyReasoning.class.getClassLoader().getResourceAsStream("adms.ttl");
        if (adms == null) {
            throw new IllegalArgumentException("File not found: " + "admsttl");
        }
        model_adms.read(adms, null, "TURTLE");

        Model model_geosparql = ModelFactory.createDefaultModel();
        InputStream geosparql = OntologyReasoning.class.getClassLoader().getResourceAsStream("geosparql_vocab_all.ttl");
        if (geosparql == null) {
            throw new IllegalArgumentException("File not found: " + "geosparql_vocab_all.ttl");
        }
        model_geosparql.read(geosparql, null, "TURTLE");

        Model model_locn = ModelFactory.createDefaultModel();
        InputStream locn = OntologyReasoning.class.getClassLoader().getResourceAsStream("locn.ttl");
        if (locn == null) {
            throw new IllegalArgumentException("File not found: " + "locn.ttl");
        }
        model_locn.read(locn, null, "TURTLE");
        Model m = model_locn.union(model_geosparql);
        return m.union(model_adms);
    }
    public Model inferTriples(Model dataModel) {
        // Load ontology from classpath
        Model ontologyModel = loadTurtleFromClasspath();

        // Combine both models
        Model combinedModel = ModelFactory.createUnion(ontologyModel, dataModel);

        InputStream ruleStream = getClass().getClassLoader().getResourceAsStream("domain-range.rules");
        if (ruleStream == null) throw new RuntimeException("Rules file not found");
        BufferedReader reader = new BufferedReader(new InputStreamReader(ruleStream));
        List<Rule> rules = Rule.parseRules(Rule.rulesParserFromReader(reader));

        // Apply a reasoner to the combined model
        //Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        //Reasoner reasoner = ReasonerRegistry.getOWLReasoner(); // or getRDFSReasoner()

        Reasoner reasoner = new GenericRuleReasoner(rules);
        reasoner.setDerivationLogging(true);  // optional

        InfModel infModel = ModelFactory.createInfModel(reasoner, combinedModel);

        return infModel.getDeductionsModel().union(dataModel);
    }
}
