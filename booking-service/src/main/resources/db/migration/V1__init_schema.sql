-- booking-service: записи на занятия (собственная БД booking_db).
-- Many to Many с дополнительными полями: участник <-> занятие.
-- member_id и class_id указывают на сущности ДРУГИХ сервисов (member-service и catalog-service),
-- поэтому внешних ключей здесь нет: существование участника и занятия проверяется на уровне
-- приложения (Feign-клиенты, шаг 5), а не на уровне БД.
CREATE TABLE bookings (
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT      NOT NULL,
    class_id  BIGINT      NOT NULL,
    booked_at TIMESTAMP   NOT NULL,
    status    VARCHAR(20) NOT NULL,
    note      VARCHAR(255),
    CONSTRAINT uq_bookings_member_class UNIQUE (member_id, class_id),
    CONSTRAINT chk_bookings_status CHECK (status IN ('BOOKED', 'CANCELLED', 'ATTENDED'))
);
CREATE INDEX idx_bookings_class ON bookings (class_id);