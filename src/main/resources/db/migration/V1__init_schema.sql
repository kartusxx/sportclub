-- Участники клуба
CREATE TABLE members (
    id         BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    phone      VARCHAR(32),
    birth_date DATE,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_members_status CHECK (status IN ('ACTIVE', 'FROZEN', 'BLOCKED'))
);

-- Спортивные направления
CREATE TABLE disciplines (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

-- Тренеры
CREATE TABLE trainers (
    id               BIGSERIAL PRIMARY KEY,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    experience_years INTEGER      NOT NULL DEFAULT 0,
    bio              VARCHAR(1000),
    CONSTRAINT chk_trainers_experience CHECK (experience_years >= 0)
);

-- Many to Many: тренер <-> направление
CREATE TABLE trainer_disciplines (
    trainer_id    BIGINT NOT NULL REFERENCES trainers (id) ON DELETE CASCADE,
    discipline_id BIGINT NOT NULL REFERENCES disciplines (id),
    PRIMARY KEY (trainer_id, discipline_id)
);

-- Тарифы абонементов
CREATE TABLE membership_plans (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL UNIQUE,
    type          VARCHAR(20)    NOT NULL,
    price         NUMERIC(10, 2) NOT NULL,
    duration_days INTEGER        NOT NULL,
    CONSTRAINT chk_plans_type CHECK (type IN ('SINGLE', 'MONTHLY', 'YEARLY')),
    CONSTRAINT chk_plans_price CHECK (price > 0),
    CONSTRAINT chk_plans_duration CHECK (duration_days > 0)
);

-- One to Many: участник -> абонементы, тариф -> абонементы
CREATE TABLE subscriptions (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT      NOT NULL REFERENCES members (id),
    plan_id    BIGINT      NOT NULL REFERENCES membership_plans (id),
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    CONSTRAINT chk_subscriptions_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_subscriptions_dates CHECK (end_date >= start_date)
);
CREATE INDEX idx_subscriptions_member ON subscriptions (member_id);
CREATE INDEX idx_subscriptions_plan ON subscriptions (plan_id);

-- One to Many: тренер -> занятия, направление -> занятия
CREATE TABLE training_classes (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(150) NOT NULL,
    description      VARCHAR(1000),
    trainer_id       BIGINT       NOT NULL REFERENCES trainers (id),
    discipline_id    BIGINT       NOT NULL REFERENCES disciplines (id),
    room             VARCHAR(50)  NOT NULL,
    starts_at        TIMESTAMP    NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    capacity         INTEGER      NOT NULL,
    level            VARCHAR(20)  NOT NULL,
    CONSTRAINT chk_classes_level CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT chk_classes_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_classes_capacity CHECK (capacity > 0)
);
CREATE INDEX idx_classes_trainer ON training_classes (trainer_id);
CREATE INDEX idx_classes_discipline ON training_classes (discipline_id);
CREATE INDEX idx_classes_starts_at ON training_classes (starts_at);

-- Many to Many с дополнительными полями: участник <-> занятие (запись на занятие)
CREATE TABLE bookings (
    id          BIGSERIAL PRIMARY KEY,
    member_id   BIGINT       NOT NULL REFERENCES members (id),
    class_id    BIGINT       NOT NULL REFERENCES training_classes (id),
    booked_at   TIMESTAMP    NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    note        VARCHAR(255),
    CONSTRAINT uq_bookings_member_class UNIQUE (member_id, class_id),
    CONSTRAINT chk_bookings_status CHECK (status IN ('BOOKED', 'CANCELLED', 'ATTENDED'))
);
CREATE INDEX idx_bookings_class ON bookings (class_id);
