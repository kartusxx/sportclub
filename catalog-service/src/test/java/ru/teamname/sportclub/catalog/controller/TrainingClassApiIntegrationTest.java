package ru.teamname.sportclub.catalog.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.AbstractIntegrationTest;
import ru.teamname.sportclub.catalog.exception.ServiceUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TrainingClassApiIntegrationTest extends AbstractIntegrationTest {

    private long disciplineId;
    private long trainerId;

    @BeforeEach
    void createReferences() {
        disciplineId = createDiscipline();
        trainerId = createTrainer(disciplineId);
    }

    @Test
    void createReturnsClassWithTrainerAndDisciplineNames() {
        web.post().uri("/api/classes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Утренняя йога", trainerId, disciplineId, 12, futureDate(3)))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.title").isEqualTo("Утренняя йога")
                .jsonPath("$.trainerName").isEqualTo("Anna Smirnova")
                .jsonPath("$.disciplineId").isEqualTo((int) disciplineId)
                .jsonPath("$.capacity").isEqualTo(12)
                .jsonPath("$.level").isEqualTo("BEGINNER");
    }

    @Test
    void getReturnsCreatedClass() {
        long id = createClass(trainerId, disciplineId, 10);

        web.get().uri("/api/classes/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo((int) id)
                .jsonPath("$.trainerName").isEqualTo("Anna Smirnova");
    }

    @Test
    void unknownClassIs404() {
        web.get().uri("/api/classes/999999").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Занятие с id=999999 не найден(а)");
    }

    @Test
    void unknownTrainerIs404() {
        web.post().uri("/api/classes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", 999999, disciplineId, 10, futureDate(3)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Тренер с id=999999 не найден(а)");
    }

    @Test
    void unknownDisciplineIs404() {
        web.post().uri("/api/classes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, 999999, 10, futureDate(3)))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Направление с id=999999 не найден(а)");
    }

    @Test
    void invalidBodyListsEveryProblem() {
        web.post().uri("/api/classes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson(" ", trainerId, disciplineId, 0, futureDate(3)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Ошибка валидации входных данных")
                .jsonPath("$.details.length()").isEqualTo(2);
    }

    @Test
    void pastStartIsRejected() {
        web.post().uri("/api/classes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, disciplineId, 10, futureDate(-1)))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.details[0]").value(detail ->
                        assertThat((String) detail).startsWith("startsAt:"));
    }

    @Test
    void unknownEnumValueIsHumanReadable400() {
        web.post().uri("/api/classes").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, disciplineId, 10, futureDate(3))
                        .replace("BEGINNER", "EXPERT"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("неверный формат"));
    }

    @Test
    void feedReportsHasNextWithoutTotalCount() {
        createClass(trainerId, disciplineId, 10);
        createClass(trainerId, disciplineId, 10);

        web.get().uri("/api/classes?page=0&size=1").exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("X-Total-Count")
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(1)
                .jsonPath("$.hasNext").isEqualTo(true)
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(1)
                .jsonPath("$.content[0].trainerName").isEqualTo("Anna Smirnova");
    }

    @Test
    void lastPageHasNoNext() {
        createClass(trainerId, disciplineId, 10);

        web.get().uri("/api/classes?page=100000&size=10").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0)
                .jsonPath("$.hasNext").isEqualTo(false);
    }

    @Test
    void feedPageSizeIsCappedAt50() {
        web.get().uri("/api/classes?size=1000").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.size").isEqualTo(50);
    }

    @Test
    void feedSupportsSortingAndRejectsUnknownField() {
        web.get().uri("/api/classes?sort=capacity,desc").exchange().expectStatus().isOk();
        web.get().uri("/api/classes?sort=room").exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateChangesFields() {
        long id = createClass(trainerId, disciplineId, 10);

        web.put().uri("/api/classes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Новое название", trainerId, disciplineId, 15, futureDate(5)))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo("Новое название")
                .jsonPath("$.capacity").isEqualTo(15);

        // вместимость выросла — booking-service не спрашиваем
        verifyNoInteractions(bookingGateway);
    }

    @Test
    void updateRejectsCapacityBelowNumberOfBookings() {
        long id = createClass(trainerId, disciplineId, 10);
        when(bookingGateway.bookedCount(id)).thenReturn(Mono.just(6L));

        web.put().uri("/api/classes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, disciplineId, 5, futureDate(3)))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("вместимость").contains("6"));

        web.get().uri("/api/classes/{id}", id).exchange()
                .expectBody().jsonPath("$.capacity").isEqualTo(10);
    }

    @Test
    void updateAllowsCapacityEqualToNumberOfBookings() {
        long id = createClass(trainerId, disciplineId, 10);
        when(bookingGateway.bookedCount(id)).thenReturn(Mono.just(5L));

        web.put().uri("/api/classes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, disciplineId, 5, futureDate(3)))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.capacity").isEqualTo(5);
    }

    @Test
    void updateReturns503WhenBookingServiceIsUnavailable() {
        long id = createClass(trainerId, disciplineId, 10);
        when(bookingGateway.bookedCount(id))
                .thenReturn(Mono.error(new ServiceUnavailableException("booking-service недоступен")));

        web.put().uri("/api/classes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, disciplineId, 5, futureDate(3)))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.status").isEqualTo(503);

        web.get().uri("/api/classes/{id}", id).exchange()
                .expectBody().jsonPath("$.capacity").isEqualTo(10);
    }

    @Test
    void updateOfUnknownClassIs404() {
        web.put().uri("/api/classes/999999").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(classJson("Йога", trainerId, disciplineId, 5, futureDate(3)))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteRemovesClassAndCleansBookings() {
        long id = createClass(trainerId, disciplineId, 10);

        web.delete().uri("/api/classes/{id}", id).exchange().expectStatus().isNoContent();

        web.get().uri("/api/classes/{id}", id).exchange().expectStatus().isNotFound();
        verify(bookingGateway).deleteBookings(id);
    }

    @Test
    void deleteOfUnknownClassIs404AndTouchesNoBookings() {
        web.delete().uri("/api/classes/999999").exchange().expectStatus().isNotFound();

        verify(bookingGateway, never()).deleteBookings(anyLong());
    }
}
