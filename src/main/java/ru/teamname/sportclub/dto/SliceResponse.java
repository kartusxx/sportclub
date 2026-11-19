package ru.teamname.sportclub.dto;

import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

/** Ответ для бесконечной прокрутки: без общего количества записей, только признак наличия следующей страницы. */
public record SliceResponse<T>(List<T> content, int page, int size, boolean hasNext) {

    public static <E, T> SliceResponse<T> from(Slice<E> slice, Function<E, T> mapper) {
        return new SliceResponse<>(
                slice.getContent().stream().map(mapper).toList(),
                slice.getNumber(),
                slice.getSize(),
                slice.hasNext());
    }
}
