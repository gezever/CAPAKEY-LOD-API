package be.vlaanderen.omgeving.capakeylodapi.reasoner;

import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.rulesys.*;
import java.util.List;

public class Reasoner {

    public Model inferTriples(Model dataModel, Model ontologyModel, List<Rule> rules) {
        // Combine both models
        Model combinedModel = ModelFactory.createUnion(ontologyModel, dataModel);
        // Apply a reasoner to the combined model
        // Alternative reasoners
        // Reasoner reasoner = ReasonerRegistry.getRDFSReasoner();
        // Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
        org.apache.jena.reasoner.Reasoner reasoner = new GenericRuleReasoner(rules);
        reasoner.setDerivationLogging(true);  // optional

        InfModel infModel = ModelFactory.createInfModel(reasoner, combinedModel);
        return infModel.getDeductionsModel().union(dataModel);
    }
}
