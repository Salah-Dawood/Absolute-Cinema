package com.cinemasaar.recommendationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Loads the real configuration and verifies HTTP validation before external calls.
@SpringBootTest
class ApplicationTest {
    @Autowired WebApplicationContext context;
    @Test void invalidPreferencesReturn400() throws Exception {
        MockMvcBuilders.webAppContextSetup(context).build().perform(post("/api/recommendations")
                .contentType(MediaType.APPLICATION_JSON).content("{\"limit\":10,\"maxDurationMinutes\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
}
