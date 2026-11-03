package ru.teamname.sportclub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.AbstractIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Справочники: направления, тренеры (Many to Many) и тарифы. */
class CatalogApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void disciplineCrudLifecycle() throws Exception {
        String name = unique("yoga");
        long id = postAndGetId("/api/disciplines", """
                {"name":"%s","description":"stretching"}
                """.formatted(name));

        mockMvc.perform(get("/api/disciplines/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        mockMvc.perform(put("/api/disciplines/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s-renamed","description":"new"}
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + "-renamed"));

        mockMvc.perform(get("/api/disciplines"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));

        mockMvc.perform(delete("/api/disciplines/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/disciplines/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void disciplineValidationAndDuplicates() throws Exception {
        mockMvc.perform(post("/api/disciplines").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        String name = unique("box");
        long id = postAndGetId("/api/disciplines", "{\"name\":\"" + name + "\"}");

        mockMvc.perform(post("/api/disciplines").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isConflict());

        long otherId = createDiscipline();
        mockMvc.perform(put("/api/disciplines/{id}", otherId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/disciplines/{id}", id)).andExpect(status().isOk());
    }

    @Test
    void trainerIsCreatedWithManyToManyDisciplines() throws Exception {
        long first = createDiscipline();
        long second = createDiscipline();
        long trainerId = createTrainer(first, second);

        mockMvc.perform(get("/api/trainers/{id}", trainerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disciplines.length()").value(2));
    }

    @Test
    void trainerUpdateReplacesDisciplines() throws Exception {
        long first = createDiscipline();
        long second = createDiscipline();
        long trainerId = createTrainer(first);

        mockMvc.perform(put("/api/trainers/{id}", trainerId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Anna","lastName":"Smirnova","email":"%s@example.com",
                                 "experienceYears":7,"disciplineIds":[%d]}
                                """.formatted(unique("renewed"), second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experienceYears").value(7))
                .andExpect(jsonPath("$.disciplines.length()").value(1))
                .andExpect(jsonPath("$.disciplines[0].id").value(second));
    }

    @Test
    void trainerWithUnknownDisciplineReturns404() throws Exception {
        mockMvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Anna","lastName":"Smirnova","email":"%s@example.com",
                                 "experienceYears":3,"disciplineIds":[987654321]}
                                """.formatted(unique("ghost"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void trainerValidationRejectsBadExperience() throws Exception {
        mockMvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Anna","lastName":"Smirnova","email":"%s@example.com",
                                 "experienceYears":-1}
                                """.formatted(unique("bad"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(1));
    }

    @Test
    void duplicateTrainerEmailReturns409() throws Exception {
        String email = unique("coach") + "@example.com";
        String json = """
                {"firstName":"Anna","lastName":"Smirnova","email":"%s","experienceYears":3}
                """.formatted(email);
        mockMvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/trainers").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void trainerListAndDelete() throws Exception {
        long trainerId = createTrainer();

        mockMvc.perform(get("/api/trainers"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));
        mockMvc.perform(delete("/api/trainers/{id}", trainerId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/trainers/{id}", trainerId)).andExpect(status().isNotFound());
    }

    @Test
    void deletingDisciplineUsedByTrainerReturns409() throws Exception {
        long disciplineId = createDiscipline();
        createTrainer(disciplineId);

        mockMvc.perform(delete("/api/disciplines/{id}", disciplineId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void planCrudLifecycle() throws Exception {
        long id = createPlan(30);

        mockMvc.perform(get("/api/plans/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MONTHLY"))
                .andExpect(jsonPath("$.durationDays").value(30));

        mockMvc.perform(put("/api/plans/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"YEARLY","price":9990.50,"durationDays":365}
                                """.formatted(unique("renamed-plan"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("YEARLY"))
                .andExpect(jsonPath("$.price").value(9990.50));

        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"));

        mockMvc.perform(delete("/api/plans/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/plans/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void planValidationRejectsNegativePriceAndMissingType() throws Exception {
        mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":-5,\"durationDays\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void duplicatePlanNameReturns409() throws Exception {
        String name = unique("same-plan");
        String json = "{\"name\":\"" + name + "\",\"type\":\"SINGLE\",\"price\":10,\"durationDays\":1}";
        mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/plans").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());

        long otherId = createPlan(10);
        mockMvc.perform(put("/api/plans/{id}", otherId).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
    }
}
