package ru.teamname.sportclub.catalog.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.teamname.sportclub.catalog.entity.Discipline;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Таблица связи trainer_disciplines (Many to Many тренер ↔ направление).
 * Spring Data R2DBC не поддерживает связи между сущностями, поэтому они читаются и пишутся вручную
 * через {@link DatabaseClient}.
 */
@Repository
@RequiredArgsConstructor
public class TrainerDisciplineRepository {

    private final DatabaseClient client;

    private record Link(Long trainerId, Discipline discipline) {
    }

    /** Направления для пачки тренеров одним запросом (без N+1), внутри списка отсортированы по названию. */
    public Mono<Map<Long, List<Discipline>>> findDisciplinesByTrainerIds(Collection<Long> trainerIds) {
        return client.sql("""
                        SELECT td.trainer_id, d.id, d.name, d.description
                        FROM trainer_disciplines td
                        JOIN disciplines d ON d.id = td.discipline_id
                        WHERE td.trainer_id IN (:ids)
                        ORDER BY d.name
                        """)
                .bind("ids", trainerIds)
                .map((row, metadata) -> {
                    Discipline discipline = new Discipline();
                    discipline.setId(row.get("id", Long.class));
                    discipline.setName(row.get("name", String.class));
                    discipline.setDescription(row.get("description", String.class));
                    return new Link(row.get("trainer_id", Long.class), discipline);
                })
                .all()
                .collect(Collectors.groupingBy(Link::trainerId,
                        Collectors.mapping(Link::discipline, Collectors.toList())));
    }

    /** Полностью заменяет набор направлений тренера. Вызывать нужно внутри транзакции. */
    public Mono<Void> replace(Long trainerId, Collection<Long> disciplineIds) {
        Mono<Void> clear = client.sql("DELETE FROM trainer_disciplines WHERE trainer_id = :trainerId")
                .bind("trainerId", trainerId)
                .fetch()
                .rowsUpdated()
                .then();
        Mono<Void> insert = Flux.fromIterable(disciplineIds)
                .concatMap(disciplineId -> client
                        .sql("INSERT INTO trainer_disciplines (trainer_id, discipline_id) "
                                + "VALUES (:trainerId, :disciplineId)")
                        .bind("trainerId", trainerId)
                        .bind("disciplineId", disciplineId)
                        .fetch()
                        .rowsUpdated())
                .then();
        return clear.then(insert);
    }
}
