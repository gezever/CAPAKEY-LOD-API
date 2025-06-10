package be.vlaanderen.omgeving.capakeylodapi.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;


/**
 *
 */
@Configuration
public class JsonldConfiguration {

    @Value("classpath:be/vlaanderen/data/id/perceel/context.json")
    private Resource contextFile;

    @Value("classpath:be/vlaanderen/data/id/perceel/frame.json")
    private Resource jsonldFrame;

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

    @Bean
    public Map getJsonLDFrame() throws IOException {
        JsonNode context = getJsonLDContext();
        ObjectMapper mapper = new ObjectMapper();
        Resource resource = loadJsonLDFrame();
        String frameStr = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        ObjectNode frame = (ObjectNode) mapper.readTree(frameStr);
        frame.set("@context", context);
        return mapper.convertValue(frame, Map.class);
    }


    private Resource loadJsonLDContext() {
        return contextFile;
    }

    private Resource loadJsonLDFrame() {
        return jsonldFrame;
    }
}
