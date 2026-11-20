package ru.teamname.sportclub.catalog.support;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.web.PageQuery;

import java.util.List;

/** Постраничное чтение таблиц через R2DBC (у реактивных репозиториев нет готового {@code Page}). */
@Component
@RequiredArgsConstructor
public class PagedReader {

    private final R2dbcEntityTemplate template;

    /** Страница с общим количеством записей: два запроса (данные и count). */
    public <T> Mono<Page<T>> readPage(Class<T> type, PageQuery query) {
        Query pageQuery = Query.empty().sort(query.sort()).limit(query.size()).offset(query.offset());
        Mono<List<T>> content = template.select(type).matching(pageQuery).all().collectList();
        Mono<Long> total = template.select(type).matching(Query.empty()).count();
        return Mono.zip(content, total)
                .<Page<T>>map(tuple -> new PageImpl<>(tuple.getT1(), query.toPageable(), tuple.getT2()));
    }

    /**
     * Срез для бесконечной прокрутки: count не выполняется. Запрашивается на одну запись больше размера
     * страницы, и её наличие говорит, есть ли следующая страница.
     */
    public <T> Mono<Slice<T>> readSlice(Class<T> type, PageQuery query) {
        Query sliceQuery = Query.empty().sort(query.sort()).limit(query.size() + 1).offset(query.offset());
        return template.select(type).matching(sliceQuery).all().collectList()
                .<Slice<T>>map(rows -> {
                    boolean hasNext = rows.size() > query.size();
                    List<T> content = hasNext ? rows.subList(0, query.size()) : rows;
                    return new SliceImpl<>(content, query.toPageable(), hasNext);
                });
    }
}
