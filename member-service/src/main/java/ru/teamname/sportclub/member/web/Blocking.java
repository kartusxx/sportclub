package ru.teamname.sportclub.member.web;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Callable;

/**
 * Мост между реактивным веб-слоем и блокирующим Spring Data JPA.
 * <p>
 * JPA и JDBC блокируют поток, поэтому вызывать их прямо в потоке Netty нельзя: это остановило бы
 * обработку всех запросов. Блокирующая операция целиком выполняется на отдельном пуле
 * {@code boundedElastic}. Вместе с ней на том же потоке выполняется и транзакция {@code @Transactional}
 * сервиса, поэтому её границы остаются прежними.
 */
public final class Blocking {

    private Blocking() {
    }

    public static <T> Mono<T> call(Callable<T> task) {
        return Mono.fromCallable(task).subscribeOn(Schedulers.boundedElastic());
    }

    public static Mono<Void> run(Runnable task) {
        return Mono.<Void>fromRunnable(task).subscribeOn(Schedulers.boundedElastic());
    }
}
