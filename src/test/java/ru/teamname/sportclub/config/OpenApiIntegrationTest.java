package ru.teamname.sportclub.config;

import org.junit.jupiter.api.Test;
import ru.teamname.sportclub.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void apiDocsDescribeAllControllers() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(org.hamcrest.Matchers.startsWith("3.")))
                .andExpect(jsonPath("$.info.title").value("SportClub API"))
                .andExpect(jsonPath("$.paths['/api/members']").exists())
                .andExpect(jsonPath("$.paths['/api/trainers']").exists())
                .andExpect(jsonPath("$.paths['/api/plans']").exists())
                .andExpect(jsonPath("$.paths['/api/subscriptions']").exists())
                .andExpect(jsonPath("$.paths['/api/classes']").exists())
                .andExpect(jsonPath("$.paths['/api/bookings']").exists());
    }
}
