package be.vlaanderen.omgeving.capakeylodapi.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;


/**
 *
 */
@Configuration
public class JsonldConfiguration {

    @Value("classpath:context.json")
    private Resource contextFile;

    @Bean
    public JsonNode getJsonLDContext() {
        Resource resource = loadJsonLDContext();
        try (InputStream inputStream = resource.getInputStream()) {
            return new ObjectMapper().readTree(inputStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Resource loadJsonLDContext() {
        return contextFile;
    }
}
