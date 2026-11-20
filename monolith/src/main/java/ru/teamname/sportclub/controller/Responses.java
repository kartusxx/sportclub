package ru.teamname.sportclub.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Общие фабрики HTTP-ответов, чтобы контроллеры не дублировали одну и ту же логику (DRY). */
final class Responses {

    static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    private Responses() {
    }

    /** 200 OK со списком записей страницы; общее количество записей передаётся в заголовке X-Total-Count. */
    static <T> ResponseEntity<List<T>> page(Page<T> page) {
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(page.getTotalElements()))
                .body(page.getContent());
    }

    /** 201 Created с заголовком Location, указывающим на созданный ресурс. */
    static <T> ResponseEntity<T> created(T body, Long id) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(body);
    }
}
