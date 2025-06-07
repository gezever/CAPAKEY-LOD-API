package be.vlaanderen.omgeving.capakeylodapi.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


/**
 *
 */
@Configuration
public class JsonldConfiguration {

    @Value("classpath:context.json")
    private Resource contextFile;

    @Value("classpath:frame.json")
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
    public Object getJsonLDFrame() throws IOException {
        Resource resource = loadJsonLDFrame();
        String frameStr = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return JsonUtils.fromString(frameStr);
    }


    private Resource loadJsonLDContext() {
        return contextFile;
    }

    private Resource loadJsonLDFrame() {
        return jsonldFrame;
    }
}
