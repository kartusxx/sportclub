package ru.teamname.sportclub.member.web;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Общие фабрики HTTP-ответов, чтобы контроллеры не дублировали одну и ту же логику (DRY). */
public final class Responses {

    public static final String TOTAL_COUNT_HEADER = "X-Total-Count";

    private Responses() {
    }

    /** 200 OK со списком записей страницы; общее количество записей передаётся в заголовке X-Total-Count. */
    public static <T> ResponseEntity<List<T>> page(Page<T> page) {
        return ResponseEntity.ok()
                .header(TOTAL_COUNT_HEADER, String.valueOf(page.getTotalElements()))
                .body(page.getContent());
    }

    /** 201 Created с заголовком Location, указывающим на созданный ресурс. */
    public static <T> ResponseEntity<T> created(T body, Long id, ServerHttpRequest request) {
        URI location = UriComponentsBuilder.fromUri(request.getURI())
                .replaceQuery(null)
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).body(body);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.<Void>noContent().build();
    }
}
