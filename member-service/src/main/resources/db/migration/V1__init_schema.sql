-- member-service: участники, тарифы и абонементы (собственная БД member_db)

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