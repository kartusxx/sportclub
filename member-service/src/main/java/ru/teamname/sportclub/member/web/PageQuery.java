package ru.teamname.sportclub.member.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.teamname.sportclub.member.exception.BadRequestException;

import java.util.Set;

/**
 * Параметры пагинации из query-строки. В WebFlux нет готового разрешителя {@code Pageable},
 * поэтому разбор выполняется здесь. Размер страницы ограничен 50 записями.
 */
public record PageQuery(int page, int size, Sort sort) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    /**
     * @param sort           значение параметра {@code sort} вида {@code поле[,asc|desc]}, может быть null
     * @param defaultSort    поле сортировки по умолчанию
     * @param allowedSorting поля, по которым разрешена сортировка
     */
    public static PageQuery of(int page, int size, String sort, String defaultSort, Set<String> allowedSorting) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        String field = defaultSort;
        Sort.Direction direction = Sort.Direction.ASC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            field = parts[0].trim();
            if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                direction = Sort.Direction.DESC;
            }
        }
        if (!allowedSorting.contains(field)) {
            throw new BadRequestException("Сортировка по полю '" + field + "' невозможна");
        }
        return new PageQuery(safePage, safeSize, Sort.by(direction, field));
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, sort);
    }

    public long offset() {
        return (long) page * size;
    }
}
