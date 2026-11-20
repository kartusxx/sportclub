package ru.teamname.sportclub.catalog.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.teamname.sportclub.catalog.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class DisciplineApiIntegrationTest extends AbstractIntegrationTest {

    private String json(String name) {
        return """
                {"name":"%s","description":"Описание"}
                """.formatted(name);
    }

    @Test
    void createReturns201WithLocationAndBody() {
        String name = unique("Йога");

        web.post().uri("/api/disciplines").contentType(MediaType.APPLICATION_JSON).bodyValue(json(name))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.name").isEqualTo(name)
                .jsonPath("$.description").isEqualTo("Описание");
    }

    @Test
    void getReturnsCreatedDiscipline() {
        String name = unique("Бокс");
        long id = postAndGetId("/api/disciplines", json(name));

        web.get().uri("/api/disciplines/{id}", id).exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo(name);
    }

    @Test
    void unknownDisciplineIsHumanReadable404() {
        web.get().uri("/api/disciplines/999999").exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Направление с id=999999 не найден(а)")
                .jsonPath("$.path").isEqualTo("/api/disciplines/999999");
    }

    @Test
    void duplicateNameIgnoringCaseIsConflict() {
        String name = unique("Pilates");
        postAndGetId("/api/disciplines", json(name));

        web.post().uri("/api/disciplines").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json(name.toUpperCase()))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("уже существует"));
    }

    @Test
    void blankNameIsValidationError() {
        web.post().uri("/api/disciplines").contentType(MediaType.APPLICATION_JSON).bodyValue(json(" "))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Ошибка валидации входных данных")
                .jsonPath("$.details[0]").value(detail ->
                        assertThat((String) detail).startsWith("name:"));
    }

    @Test
    void malformedJsonIsBadRequest() {
        web.post().uri("/api/disciplines").contentType(MediaType.APPLICATION_JSON).bodyValue("{не json")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void updateChangesNameAndAllowsKeepingOwnName() {
        String name = unique("Crossfit");
        long id = postAndGetId("/api/disciplines", json(name));

        // то же имя у самого направления — не конфликт
        web.put().uri("/api/disciplines/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\":\"" + name + "\",\"description\":\"Новое\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.description").isEqualTo("Новое");

        String renamed = unique("Functional");
        web.put().uri("/api/disciplines/{id}", id).contentType(MediaType.APPLICATION_JSON).bodyValue(json(renamed))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.name").isEqualTo(renamed);
    }

    @Test
    void updateToNameOfAnotherDisciplineIsConflict() {
        String taken = unique("Taken");
        postAndGetId("/api/disciplines", json(taken));
        long id = postAndGetId("/api/disciplines", json(unique("Other")));

        web.put().uri("/api/disciplines/{id}", id).contentType(MediaType.APPLICATION_JSON).bodyValue(json(taken))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void deleteReturns204ThenDisciplineIsGone() {
        long id = createDiscipline();

        web.delete().uri("/api/disciplines/{id}", id).exchange().expectStatus().isNoContent();
        web.get().uri("/api/disciplines/{id}", id).exchange().expectStatus().isNotFound();
        web.delete().uri("/api/disciplines/{id}", id).exchange().expectStatus().isNotFound();
    }

    @Test
    void disciplineUsedByTrainerCannotBeDeleted() {
        long disciplineId = createDiscipline();
        createTrainer(disciplineId);

        web.delete().uri("/api/disciplines/{id}", disciplineId).exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").value(message ->
                        assertThat((String) message).contains("ссылаются"));
    }

    @Test
    void listIsPagedWithTotalCountHeaderAndSizeCap() {
        for (int i = 0; i < 3; i++) {
            createDiscipline();
        }

        web.get().uri("/api/disciplines?page=0&size=2").exchange()
                .expectStatus().isOk()
                .expectHeader().value("X-Total-Count", total ->
                        assertThat(Long.parseLong(total)).isGreaterThanOrEqualTo(3))
                .expectBody().jsonPath("$.length()").isEqualTo(2);

        // размер страницы ограничен 50 записями
        web.get().uri("/api/disciplines?size=1000").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").value(length ->
                        assertThat((Integer) length).isLessThanOrEqualTo(50));
    }

    @Test
    void listSupportsSortingAndRejectsUnknownSortField() {
        web.get().uri("/api/disciplines?sort=name,desc").exchange().expectStatus().isOk();

        web.get().uri("/api/disciplines?sort=secret").exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Сортировка по полю 'secret' невозможна");
    }
}
