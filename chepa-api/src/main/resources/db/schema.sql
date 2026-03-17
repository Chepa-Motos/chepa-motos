-- =============================================================
-- Chepa Motos — schema.sql
-- PostgreSQL 18
-- Ejecutar antes de seed.sql
-- =============================================================

-- -------------------------------------------------------------
-- EXTENSIONES
-- -------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- -------------------------------------------------------------
-- TIPOS
-- -------------------------------------------------------------
CREATE TYPE invoice_type AS ENUM ('SERVICE', 'DELIVERY');

-- -------------------------------------------------------------
-- TABLAS
-- -------------------------------------------------------------

CREATE TABLE mechanic (
    mechanic_id     BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT true
);

CREATE TABLE vehicle (
    vehicle_id      BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    plate           VARCHAR(20)     NOT NULL UNIQUE,
    model           VARCHAR(100)    NOT NULL,


);

CREATE TABLE invoice (
    invoice_id      BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_type    invoice_type    NOT NULL,
    mechanic_id     BIGINT          REFERENCES mechanic (mechanic_id),
    vehicle_id      BIGINT          REFERENCES vehicle (vehicle_id),
    buyer_name      VARCHAR(150),
    created_at      TIMESTAMP       NOT NULL DEFAULT now(),
    labor_amount    NUMERIC(10,2)   NOT NULL DEFAULT 0,
    total_amount    NUMERIC(10,2)   NOT NULL DEFAULT 0,
    is_cancelled    BOOLEAN         NOT NULL DEFAULT false,

    CONSTRAINT chk_invoice_type CHECK (
        (
            invoice_type = 'SERVICE'
            AND mechanic_id IS NOT NULL
            AND vehicle_id  IS NOT NULL
            AND buyer_name  IS NULL
        )
        OR
        (
            invoice_type  = 'DELIVERY'
            AND buyer_name   IS NOT NULL
            AND mechanic_id  IS NULL
            AND vehicle_id   IS NULL
            AND labor_amount = 0
        )
    )
);

CREATE TABLE invoice_item (
    invoice_item_id BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    invoice_id      BIGINT          NOT NULL REFERENCES invoice (invoice_id),
    description     VARCHAR(255)    NOT NULL,
    quantity        NUMERIC(10,2)   NOT NULL,
    unit_price      NUMERIC(10,2)   NOT NULL,
    subtotal        NUMERIC(10,2)   NOT NULL
);

CREATE TABLE daily_liquidation (
    daily_liquidation_id    BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mechanic_id             BIGINT          NOT NULL REFERENCES mechanic (mechanic_id),
    date                    DATE            NOT NULL,
    total_revenue           NUMERIC(10,2)   NOT NULL,
    mechanic_share          NUMERIC(10,2)   NOT NULL,
    shop_share              NUMERIC(10,2)   NOT NULL,
    invoice_count           INTEGER         NOT NULL,

    CONSTRAINT uq_liquidation_mechanic_date
        UNIQUE (mechanic_id, date)
);

-- -------------------------------------------------------------
-- ÍNDICES
-- -------------------------------------------------------------

-- invoice
CREATE INDEX idx_invoice_mechanic_id   ON invoice (mechanic_id);
CREATE INDEX idx_invoice_vehicle_id    ON invoice (vehicle_id);
CREATE INDEX idx_invoice_created_at    ON invoice (created_at);
CREATE INDEX idx_invoice_is_cancelled  ON invoice (is_cancelled);

-- vehicle
CREATE INDEX idx_vehicle_plate         ON vehicle (plate);

-- invoice_item — trigrama para autocomplete case-insensitive
CREATE INDEX idx_invoice_item_description_trgm
    ON invoice_item USING GIN (LOWER(description) gin_trgm_ops);
