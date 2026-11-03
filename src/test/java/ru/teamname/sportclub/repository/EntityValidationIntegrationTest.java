package ru.teamname.sportclub.repository;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.teamname.sportclub.AbstractIntegrationTest;
import ru.teamname.sportclub.entity.Member;
import ru.teamname.sportclub.entity.MembershipPlan;
import ru.teamname.sportclub.enums.MemberStatus;
import ru.teamname.sportclub.enums.PlanType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Валидация на уровне Entity и ограничения БД, созданные Flyway-миграцией. */
class EntityValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static Member validMember(String email) {
        Member member = new Member();
        member.setFirstName("Ivan");
        member.setLastName("Petrov");
        member.setEmail(email);
        member.setStatus(MemberStatus.ACTIVE);
        return member;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void invalidEmailIsRejectedByEntityValidation() {
        Member member = validMember("not-an-email");

        assertThatThrownBy(() -> memberRepository.saveAndFlush(member))
                .satisfies(ex -> assertThat(hasCause(ex, ConstraintViolationException.class)).isTrue());
    }

    @Test
    void blankNameAndFutureBirthDateAreRejectedByEntityValidation() {
        Member member = validMember(unique("bad") + "@example.com");
        member.setFirstName(" ");
        member.setBirthDate(LocalDate.now().plusYears(1));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(member))
                .satisfies(ex -> assertThat(hasCause(ex, ConstraintViolationException.class)).isTrue());
    }

    @Test
    void negativePriceIsRejectedByEntityValidation() {
        MembershipPlan plan = new MembershipPlan();
        plan.setName(unique("cheap"));
        plan.setType(PlanType.SINGLE);
        plan.setPrice(new BigDecimal("-1.00"));
        plan.setDurationDays(1);

        assertThatThrownBy(() -> planRepository.saveAndFlush(plan))
                .satisfies(ex -> assertThat(hasCause(ex, ConstraintViolationException.class)).isTrue());
    }

    @Test
    void uniqueEmailConstraintComesFromMigration() {
        String email = unique("unique") + "@example.com";
        memberRepository.saveAndFlush(validMember(email));

        assertThatThrownBy(() -> memberRepository.saveAndFlush(validMember(email)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enumIsStoredAsString() {
        Member member = validMember(unique("enum") + "@example.com");
        member.setStatus(MemberStatus.FROZEN);

        Member saved = memberRepository.saveAndFlush(member);

        String raw = jdbcTemplate.queryForObject(
                "select status from members where id = ?", String.class, saved.getId());
        assertThat(raw).isEqualTo("FROZEN");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
