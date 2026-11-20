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

class TrainingClassApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void feedReturnsSliceWithoutTotalCount() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        for (int i = 0; i < 3; i++) {
            createClass(trainerId, disciplineId, 10, futureStart().plusHours(i));
        }

        mockMvc.perform(get("/api/classes").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.totalElements").doesNotExist());
    }

    @Test
    void feedSizeIsCappedAt50() throws Exception {
        mockMvc.perform(get("/api/classes").param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void createReturns201AndResolvesTrainerAndDiscipline() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        long classId = createClass(trainerId, disciplineId, 15, futureStart());

        mockMvc.perform(get("/api/classes/{id}", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerId").value(trainerId))
                .andExpect(jsonPath("$.disciplineId").value(disciplineId))
                .andExpect(jsonPath("$.level").value("BEGINNER"))
                .andExpect(jsonPath("$.capacity").value(15));
    }

    @Test
    void createWithPastStartAndBadCapacityReturns400() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        String json = """
                {"title":"Old class","trainerId":%d,"disciplineId":%d,"room":"Hall",
                 "startsAt":"2000-01-01T10:00:00","durationMinutes":60,"capacity":0,"level":"BEGINNER"}
                """.formatted(trainerId, disciplineId);

        mockMvc.perform(post("/api/classes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.length()").value(2));
    }

    @Test
    void createWithUnknownTrainerReturns404() throws Exception {
        long disciplineId = createDiscipline();
        String json = """
                {"title":"Ghost class","trainerId":987654321,"disciplineId":%d,"room":"Hall",
                 "startsAt":"%s","durationMinutes":60,"capacity":10,"level":"ADVANCED"}
                """.formatted(disciplineId, futureStart());

        mockMvc.perform(post("/api/classes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateChangesFieldsAndUnknownIdReturns404() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        long classId = createClass(trainerId, disciplineId, 10, futureStart());
        String json = """
                {"title":"Renamed","trainerId":%d,"disciplineId":%d,"room":"Hall 2",
                 "startsAt":"%s","durationMinutes":90,"capacity":20,"level":"INTERMEDIATE"}
                """.formatted(trainerId, disciplineId, futureStart().plusDays(1));

        mockMvc.perform(put("/api/classes/{id}", classId).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.level").value("INTERMEDIATE"));

        mockMvc.perform(put("/api/classes/{id}", 987654321L).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateCapacityBelowBookedCountReturns422() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        long classId = createClass(trainerId, disciplineId, 5, futureStart());
        long planId = createPlan(30);
        for (int i = 0; i < 2; i++) {
            long memberId = createMember();
            purchaseSubscription(memberId, planId);
            postAndGetId("/api/bookings", bookingJson(memberId, classId));
        }
        String json = """
                {"title":"Shrunk","trainerId":%d,"disciplineId":%d,"room":"Hall",
                 "startsAt":"%s","durationMinutes":60,"capacity":1,"level":"BEGINNER"}
                """.formatted(trainerId, disciplineId, futureStart());

        mockMvc.perform(put("/api/classes/{id}", classId).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deleteRemovesClassTogetherWithItsBookings() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        long classId = createClass(trainerId, disciplineId, 5, futureStart());
        long memberId = createMember();
        purchaseSubscription(memberId, createPlan(30));
        long bookingId = postAndGetId("/api/bookings", bookingJson(memberId, classId));

        mockMvc.perform(delete("/api/classes/{id}", classId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/classes/{id}", classId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/bookings/{id}", bookingId)).andExpect(status().isNotFound());
    }

    @Test
    void deleteUnknownClassReturns404() throws Exception {
        mockMvc.perform(delete("/api/classes/{id}", 987654321L)).andExpect(status().isNotFound());
    }

    @Test
    void deletingTrainerWithClassesReturns409() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        createClass(trainerId, disciplineId, 5, futureStart());

        mockMvc.perform(delete("/api/trainers/{id}", trainerId)).andExpect(status().isConflict());
    }

    @Test
    void classBookingsEndpointReturnsPageWithTotalCount() throws Exception {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        long classId = createClass(trainerId, disciplineId, 5, futureStart());
        long memberId = createMember();
        purchaseSubscription(memberId, createPlan(30));
        postAndGetId("/api/bookings", bookingJson(memberId, classId));

        mockMvc.perform(get("/api/classes/{id}/bookings", classId))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$[0].classId").value(classId));

        mockMvc.perform(get("/api/classes/{id}/bookings", 987654321L)).andExpect(status().isNotFound());
    }
}
