package ru.teamname.sportclub.booking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.booking.AbstractMockedDirectoriesTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Внутренний API, которым пользуются member-service и catalog-service (через gateway недоступен). */
class InternalBookingApiIntegrationTest extends AbstractMockedDirectoriesTest {

    @Test
    void bookedCountCountsOnlyActiveBookings() throws Exception {
        long classId = newClass(10);
        postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        long cancelled = postAndGetId("/api/bookings", bookingJson(newMember(), classId));
        mockMvc.perform(post("/api/bookings/{id}/cancel", cancelled)).andExpect(status().isOk());

        mockMvc.perform(get("/internal/bookings/class/{id}/booked-count", classId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void bookedCountOfClassWithoutBookingsIsZero() throws Exception {
        mockMvc.perform(get("/internal/bookings/class/{id}/booked-count", newClass(10)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void memberHasBookingsInAnyStatus() throws Exception {
        long memberId = newMember();
        long other = newMember();
        long bookingId = postAndGetId("/api/bookings", bookingJson(memberId, newClass(10)));

        mockMvc.perform(get("/internal/bookings/member/{id}/exists", memberId))
                .andExpect(jsonPath("$.exists").value(true));
        mockMvc.perform(get("/internal/bookings/member/{id}/exists", other))
                .andExpect(jsonPath("$.exists").value(false));

        // отменённая запись всё равно ссылается на участника
        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)).andExpect(status().isOk());
        mockMvc.perform(get("/internal/bookings/member/{id}/exists", memberId))
                .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void deleteByClassRemovesOnlyBookingsOfThatClassAndIsIdempotent() throws Exception {
        long classId = newClass(10);
        long otherClass = newClass(10);
        long memberId = newMember();
        long removed = postAndGetId("/api/bookings", bookingJson(memberId, classId));
        long kept = postAndGetId("/api/bookings", bookingJson(memberId, otherClass));

        mockMvc.perform(delete("/internal/bookings/class/{id}", classId)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/internal/bookings/class/{id}", classId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/bookings/{id}", removed)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/bookings/{id}", kept))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classId").value(otherClass));
    }

    @Test
    void internalEndpointsRejectWrongPathVariables() throws Exception {
        mockMvc.perform(get("/internal/bookings/class/abc/booked-count").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
