package ru.teamname.sportclub.member.web;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import ru.teamname.sportclub.member.exception.BadRequestException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageQueryTest {

    private static final Set<String> ALLOWED = Set.of("id", "email");

    @Test
    void usesDefaultsWhenNothingIsGiven() {
        PageQuery query = PageQuery.of(0, 20, null, "id", ALLOWED);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
        assertThat(query.offset()).isZero();
    }

    @Test
    void limitsPageSizeTo50AndFixesInvalidValues() {
        assertThat(PageQuery.of(0, 1000, null, "id", ALLOWED).size()).isEqualTo(50);
        assertThat(PageQuery.of(-5, 0, null, "id", ALLOWED).page()).isZero();
        assertThat(PageQuery.of(-5, 0, null, "id", ALLOWED).size()).isEqualTo(PageQuery.DEFAULT_SIZE);
    }

    @Test
    void parsesSortFieldAndDirection() {
        PageQuery query = PageQuery.of(2, 10, "email,desc", "id", ALLOWED);

        assertThat(query.sort()).isEqualTo(Sort.by(Sort.Direction.DESC, "email"));
        assertThat(query.offset()).isEqualTo(20);
        assertThat(query.toPageable().getPageNumber()).isEqualTo(2);
    }

    @Test
    void rejectsSortingByUnknownField() {
        assertThatThrownBy(() -> PageQuery.of(0, 10, "password", "id", ALLOWED))
                .isInstanceOf(BadRequestException.class);
    }
}
