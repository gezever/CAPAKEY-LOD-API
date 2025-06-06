package be.vlaanderen.omgeving.capakeylodapi.configuration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.Rule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 *
 */
@Configuration
public class ReasoningModelConfiguration {

    @Value("classpath:domain-range.rules")
    private Resource rules;

    @Value("classpath:adms.ttl")
    private Resource adms;

    @Value("classpath:geosparql_vocab_all.ttl")
    private Resource geosparql;

    @Value("classpath:locn.ttl")
    private Resource locn;

    @Bean
    public  Model loadTurtleFromClasspath() {
        Model model_adms = ModelFactory.createDefaultModel();
        Resource adms = loadAdms();
        try {
            model_adms.read(adms.getInputStream(), null, "TURTLE");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Model model_geosparql = ModelFactory.createDefaultModel();
        Resource geosparql = loadGeosparql();
        try {
            model_geosparql.read(geosparql.getInputStream(), null, "TURTLE");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Model model_locn = ModelFactory.createDefaultModel();
        Resource locn = loadLocn();
        try {
            model_locn.read(locn.getInputStream(), null, "TURTLE");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Model m = model_locn.union(model_geosparql);
        return m.union(model_adms);
    }

    @Bean
    public List<Rule> getRules() {
        Resource ruleresource = loadRules();
        try {
            InputStream ruleStream = ruleresource.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(ruleStream));
            return Rule.parseRules(Rule.rulesParserFromReader(reader));
        } catch (IOException e) {
            throw new RuntimeException("Failed to construct rules", e);
        }
    }

    private Resource loadLocn() {
        return locn;
    }

    private Resource loadGeosparql() {
        return geosparql;
    }

    private Resource loadAdms() {
        return adms;
    }

    private Resource loadRules() {
        return rules;
    }
}
