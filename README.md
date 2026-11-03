# SportClub

Backend-система управления спортивным клубом (лабораторная работа №1 — монолит на Spring Boot).

**Стек:** Java 21, Spring Boot, Spring Data JPA, PostgreSQL, Flyway, springdoc-openapi (Swagger UI),
JUnit 5, Testcontainers, JaCoCo, Maven, Docker Compose.

## Запуск

```bash
docker compose up --build
```

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Конфигурация задаётся переменными среды (см. `environment:` в `docker-compose.yml`):

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `SERVER_PORT` | `8080` | порт приложения |
| `DB_URL` | `jdbc:postgresql://localhost:5432/sportclub` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `sportclub` / `sportclub` | доступ к БД |
| `APP_PORT`, `POSTGRES_*` | — | параметры compose-стенда |

## Тесты

```bash
./mvnw verify
```

Нужен запущенный Docker (интеграционные тесты поднимают PostgreSQL через Testcontainers).
JaCoCo проверяет покрытие строк: сборка падает, если оно ниже 70 %. Отчёт — `target/site/jacoco/index.html`.

## Модель данных

| Сущность | Связи |
|---|---|
| `Member` (участник) | 1—N `Subscription`; N—M `TrainingClass` через `Booking` |
| `MembershipPlan` (тариф) | 1—N `Subscription` |
| `Subscription` (абонемент) | N—1 `Member`, N—1 `MembershipPlan` |
| `Discipline` (направление) | N—M `Trainer`; 1—N `TrainingClass` |
| `Trainer` (тренер) | N—M `Discipline` (`trainer_disciplines`); 1—N `TrainingClass` |
| `TrainingClass` (занятие) | N—1 `Trainer`, N—1 `Discipline`; N—M `Member` через `Booking` |
| `Booking` (запись) | **Many to Many с доп. полями** `booked_at`, `status`, `note` |

Типы связей: **Many to Many** — `Trainer` ↔ `Discipline`; **One to Many / Many to One** — `Member` → `Subscription`,
`Trainer` → `TrainingClass` и др.; **Many to Many с дополнительным полем** — `Member` ↔ `TrainingClass` через `Booking`.
Схема создаётся миграцией Flyway `V1__init_schema.sql`; все enum хранятся в БД строками (`EnumType.STRING` + `CHECK`).

## REST API

Префикс `/api`: `members`, `disciplines`, `trainers`, `plans`, `subscriptions`, `classes`, `bookings` —
полный CRUD, коды ответов 200/201/204/400/404/409. Ошибки возвращаются в виде JSON с читаемым сообщением
(`GlobalExceptionHandler`). Entity и DTO разделены (`dto`, `mapper`).

### Пагинация

Каждый `findAll` постраничный, размер страницы ограничен **50** (`spring.data.web.pageable.max-page-size`).

- **С общим количеством** — большинство списков: `GET /api/members?page=0&size=20`;
  общее число записей приходит в заголовке `X-Total-Count`.
- **Бесконечная прокрутка без total** — `GET /api/classes` возвращает `Slice` (`content`, `hasNext`),
  запрос `count(*)` не выполняется.

### Транзакции

Составные операции выполняются в одной `@Transactional`-транзакции:

1. **Запись на занятие** (`POST /api/bookings`, `BookingService.book`). Проверки (абонемент, дубликат, свободные места)
   и вставка записи должны быть атомарны. Строка занятия блокируется `PESSIMISTIC_WRITE`, иначе два параллельных запроса
   займут последнее место (овербукинг).
2. **Оформление абонемента** (`POST /api/subscriptions`, `SubscriptionService.purchase`). Одновременно переводятся в
   `EXPIRED` просроченные абонементы, считается дата начала (продление после действующего) и создаётся новый.
   Блокировка участника исключает одинаковые даты при параллельных покупках, а откат не оставляет частичных изменений.
3. **Удаление занятия** (`TrainingClassService`) — записи на занятие и само занятие удаляются атомарно.

## CI

`.github/workflows/ci.yml` запускает `./mvnw verify` (тесты + проверка покрытия 70 %) на каждый push и pull request.

## Git-flow

Разработка ведётся в feature-ветках (`feature/*`), коммиты — по Conventional Commits.
