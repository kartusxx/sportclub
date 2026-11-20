package ru.teamname.sportclub.catalog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.catalog.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class TrainerApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createReturnsTrainerWithDisciplinesSortedByName() {
        long yoga = postAndGetId("/api/disciplines", "{\"name\":\"" + unique("Я-йога") + "\"}");
        long aikido = postAndGetId("/api/disciplines", "{\"name\":\"" + unique("А-айкидо") + "\"}");

        web.post().uri("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(unique("t") + "@example.com", yoga + "," + aikido))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.disciplines.length()").isEqualTo(2)
                .jsonPath("$.disciplines[0].id").isEqualTo((int) aikido)
                .jsonPath("$.disciplines[1].id").isEqualTo((int) yoga);
    }

    @Test
    void createWithoutDisciplinesIsAllowed() {
        web.post().uri("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"firstName":"Oleg","lastName":"Ivanov","email":"%s","experienceYears":0}
                        """.formatted(unique("t") + "@example.com"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.disciplines.length()").isEqualTo(0);
    }

    @Test
    void getReturnsTrainerWithDisciplines() {
        long disciplineId = createDiscipline();
        long id = createTrainer(disciplineId);

        web.get().uri("/api/trainers/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("Anna")
                .jsonPath("$.disciplines[0].id").isEqualTo((int) disciplineId);
    }

    @Test
    void unknownTrainerIs404() {
        web.get().uri("/api/trainers/999999").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Тренер с id=999999 не найден(а)");
    }

    @Test
    void unknownDisciplineInRequestIs404AndTrainerIsNotCreated() {
        String email = unique("ghost") + "@example.com";

        web.post().uri("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(email, "999999"))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Направление с id=999999 не найден(а)");

        // тренер не создан: тот же email свободен
        web.post().uri("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(email, ""))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void duplicateEmailIgnoringCaseIsConflict() {
        String email = unique("dup") + "@example.com";
        postAndGetId("/api/trainers", trainerJson(email, ""));

        web.post().uri("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(email.toUpperCase(), ""))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("уже существует"));
    }

    @Test
    void invalidBodyListsEveryProblem() {
        web.post().uri("/api/trainers").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"firstName":"","lastName":"Ivanov","email":"not-an-email","experienceYears":100}
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Ошибка валидации входных данных")
                .jsonPath("$.details.length()").isEqualTo(3);
    }

    @Test
    void updateReplacesDisciplineSet() {
        long first = createDiscipline();
        long second = createDiscipline();
        long id = createTrainer(first);

        web.put().uri("/api/trainers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"firstName":"Anna","lastName":"Novaya","email":"%s","experienceYears":7,
                         "disciplineIds":[%d]}
                        """.formatted(unique("upd") + "@example.com", second))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.lastName").isEqualTo("Novaya")
                .jsonPath("$.disciplines.length()").isEqualTo(1)
                .jsonPath("$.disciplines[0].id").isEqualTo((int) second);

        web.get().uri("/api/trainers/{id}", id).exchange()
                .expectBody().jsonPath("$.disciplines[0].id").isEqualTo((int) second);
    }

    @Test
    void updateKeepingOwnEmailIsNotConflict() {
        String email = unique("same") + "@example.com";
        long id = postAndGetId("/api/trainers", trainerJson(email, ""));

        web.put().uri("/api/trainers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(email, ""))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateToEmailOfAnotherTrainerIsConflict() {
        String taken = unique("taken") + "@example.com";
        postAndGetId("/api/trainers", trainerJson(taken, ""));
        long id = createTrainer();

        web.put().uri("/api/trainers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(taken, ""))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void failedUpdateKeepsPreviousDisciplines() {
        // Транзакция: ошибка на неизвестном направлении не должна стереть прежние связи
        long disciplineId = createDiscipline();
        long id = createTrainer(disciplineId);

        web.put().uri("/api/trainers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(trainerJson(unique("x") + "@example.com", "999999"))
                .exchange()
                .expectStatus().isNotFound();

        web.get().uri("/api/trainers/{id}", id).exchange()
                .expectBody().jsonPath("$.disciplines[0].id").isEqualTo((int) disciplineId);
    }

    @Test
    void deleteRemovesTrainerAndLinksButKeepsDiscipline() {
        long disciplineId = createDiscipline();
        long id = createTrainer(disciplineId);

        web.delete().uri("/api/trainers/{id}", id).exchange().expectStatus().isNoContent();

        web.get().uri("/api/trainers/{id}", id).exchange().expectStatus().isNotFound();
        web.get().uri("/api/disciplines/{id}", disciplineId).exchange().expectStatus().isOk();
        // связь удалена каскадно, значит направление теперь можно удалить
        web.delete().uri("/api/disciplines/{id}", disciplineId).exchange().expectStatus().isNoContent();
    }

    @Test
    void trainerWithClassesCannotBeDeleted() {
        long disciplineId = createDiscipline();
        long trainerId = createTrainer(disciplineId);
        createClass(trainerId, disciplineId, 10);

        web.delete().uri("/api/trainers/{id}", trainerId).exchange().expectStatus().isEqualTo(409);
        web.get().uri("/api/trainers/{id}", trainerId).exchange().expectStatus().isOk();
    }

    @Test
    void listIsPagedWithTotalCountAndAttachedDisciplines() {
        long disciplineId = createDiscipline();
        createTrainer(disciplineId);
        createTrainer(disciplineId);

        web.get().uri("/api/trainers?page=0&size=1&sort=id,desc").exchange()
                .expectStatus().isOk()
                .expectHeader().value("X-Total-Count", total ->
                        assertThat(Long.parseLong(total)).isGreaterThanOrEqualTo(2))
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].disciplines.length()").isEqualTo(1);
    }

    @Test
    void listRejectsUnknownSortField() {
        web.get().uri("/api/trainers?sort=password").exchange().expectStatus().isBadRequest();
    }
}
