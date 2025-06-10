package be.vlaanderen.omgeving.capakeylodapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PerceelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testJsonResponse() throws Exception {
        mockMvc.perform(get("/id/perceel/24504D0693/00B000")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.capakey").value("24504D0693/00B000"))
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void testJsonLdResponse() throws Exception {
        mockMvc.perform(get("/id/perceel/24504D0693/00B000")
                .accept("application/ld+json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/ld+json"))
                .andExpect(jsonPath("identifier.capakey").value("24504D0693/00B000"))
                .andExpect(jsonPath("geometry.type").value("geo:Geometry"));
    }

    @Test
    void testTurtleResponse() throws Exception {
        mockMvc.perform(get("/id/perceel/24504D0693/00B000")
                .accept("text/turtle"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/turtle"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("geo:asWKT")));
    }

    @Test
    void testXmlRdfResponse() throws Exception {
        mockMvc.perform(get("/id/perceel/24504D0693/00B000")
                .accept("application/rdf+xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rdf+xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("geo:asWKT")));
    }

    @Test
    void testInternalServerError() throws Exception {
        mockMvc.perform(get("/id/perceel/FAKE/12345")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("An error has occurred")));
    }

    @Test
    void testUnsupportedAcceptHeader() throws Exception {
        mockMvc.perform(get("/id/perceel/24504D0693/00B000")
                .accept("application/xml"))
                .andExpect(status().isNotAcceptable());
    }
}
