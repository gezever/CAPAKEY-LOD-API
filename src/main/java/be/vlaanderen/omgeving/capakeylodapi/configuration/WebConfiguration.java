package be.vlaanderen.omgeving.capakeylodapi.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 *
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {


    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("json", MediaType.APPLICATION_JSON);
        configurer.mediaType("jsonld", MediaType.valueOf("application/ld+json"));
        configurer.mediaType("ttl", MediaType.valueOf("text/turtle"));
        configurer.mediaType("rdf", MediaType.valueOf("application/rdf+xml"));

        configurer.favorParameter(true);
    }

}
