-- catalog-service: направления, тренеры и занятия (собственная БД catalog_db)

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