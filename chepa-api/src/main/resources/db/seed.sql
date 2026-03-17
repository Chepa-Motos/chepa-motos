-- =============================================================
-- Chepa Motos — seed.sql
-- PostgreSQL 18
-- Ejecutar después de schema.sql
-- =============================================================
-- Datos semilla representativos:
--   5 mecánicos (1 inactivo)
--  10 vehículos
--  20 facturas SERVICE con ítems variados
--   5 facturas DELIVERY
--   4 liquidaciones diarias (dos días, dos mecánicos cada día)
-- =============================================================

-- -------------------------------------------------------------
-- MECÁNICOS
-- -------------------------------------------------------------
INSERT INTO mechanic (name, is_active) VALUES
    ('Jose',    true),   -- mechanic_id = 1
    ('Andrés',  true),   -- mechanic_id = 2
    ('Carlos',  true),   -- mechanic_id = 3
    ('Julián',  true),   -- mechanic_id = 4
    ('Memo',    false);  -- mechanic_id = 5 — inactivo

-- -------------------------------------------------------------
-- VEHÍCULOS
-- -------------------------------------------------------------
INSERT INTO vehicle (plate, model) VALUES
    ('BXR42H', 'Boxer 150 2021'),     -- vehicle_id = 1
    ('YMH19F', 'Yamaha FZ 2022'),     -- vehicle_id = 2
    ('HND08B', 'Honda CB 2020'),      -- vehicle_id = 3
    ('KTM45D', 'KTM Duke 2023'),      -- vehicle_id = 4
    ('SZK30G', 'Suzuki GN 2019'),     -- vehicle_id = 5
    ('AKT22C', 'AKT 125 2022'),       -- vehicle_id = 6
    ('TVS11E', 'TVS Apache 2021'),    -- vehicle_id = 7
    ('BXR55A', 'Boxer 125 2020'),     -- vehicle_id = 8
    ('YMH77H', 'Yamaha Crux 2023'),   -- vehicle_id = 9
    ('HND33F', 'Honda Wave 2022');    -- vehicle_id = 10

-- -------------------------------------------------------------
-- FACTURAS SERVICE — día 1 (2026-01-27)
-- -------------------------------------------------------------

-- Factura 1 — Jose, Boxer 150
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 1, 1, '2026-01-27 09:15:00', 45000.00, 98900.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (1, 'Freno delantero',     1, 36900.00, 36900.00),
    (1, 'Pastillas de freno',  2,  8500.00, 17000.00);

-- Factura 2 — Jose, Yamaha FZ
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 1, 2, '2026-01-27 10:30:00', 60000.00, 149900.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (2, 'Filtro de aceite',    1, 18900.00, 18900.00),
    (2, 'Aceite 20W50 1L',     2, 22500.00, 45000.00),
    (2, 'Bujía',               1, 26000.00, 26000.00);

-- Factura 3 — Andrés, KTM Duke
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 2, 4, '2026-01-27 09:45:00', 80000.00, 209100.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (3, 'Tornillo leva',       1,  3900.00,  3900.00),
    (3, 'Prensa tapa discos',  1, 29900.00, 29900.00),
    (3, 'Discos',              1, 36900.00, 36900.00),
    (3, 'Canastilla',          1,  8900.00,  8900.00),
    (3, 'Selector',            1,  7900.00,  7900.00),
    (3, 'Cadena transmisión',  1, 41600.00, 41600.00);

-- Factura 4 — Andrés, Honda CB
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 2, 3, '2026-01-27 11:00:00', 35000.00, 88500.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (4, 'Cable freno trasero', 1, 22500.00, 22500.00),
    (4, 'Manija freno',        1, 31000.00, 31000.00);

-- Factura 5 — Carlos, Suzuki GN
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 3, 5, '2026-01-27 10:00:00', 40000.00, 112000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (5, 'Llanta delantera',    1, 72000.00, 72000.00);

-- Factura 6 — Carlos, AKT 125
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 3, 6, '2026-01-27 14:00:00', 30000.00, 74900.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (6, 'Filtro de aire',      1, 18900.00, 18900.00),
    (6, 'Bujía',               1, 26000.00, 26000.00);

-- Factura 7 — Julián, TVS Apache — ANULADA
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 4, 7, '2026-01-27 11:30:00', 50000.00, 88500.00, true);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (7, 'Freno trasero',       1, 38500.00, 38500.00);

-- Factura 8 — Julián, Boxer 125
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 4, 8, '2026-01-27 15:00:00', 25000.00, 62900.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (8, 'Palanca de freno',    1, 18500.00, 18500.00),
    (8, 'Tornillo leva',       2,  3900.00,  7800.00),
    (8, 'Manija acelerador',   1, 11600.00, 11600.00);

-- -------------------------------------------------------------
-- FACTURAS SERVICE — día 2 (2026-01-28)
-- -------------------------------------------------------------

-- Factura 9 — Jose, Boxer 150 (cliente recurrente)
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 1, 1, '2026-01-28 09:00:00', 65000.00, 209200.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (9,  'Tornillo leva',      1,  3900.00,  3900.00),
    (9,  'Prensa tapa discos', 1, 29900.00, 29900.00),
    (9,  'Discos',             1, 36900.00, 36900.00),
    (9,  'Canastilla',         1,  8900.00,  8900.00),
    (9,  'Selector',           1,  7900.00,  7900.00),
    (9,  'Cadena transmisión', 1, 56700.00, 56700.00);

-- Factura 10 — Jose, Yamaha Crux
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 1, 9, '2026-01-28 10:45:00', 35000.00, 95000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (10, 'Filtro de aceite',   1, 18900.00, 18900.00),
    (10, 'Aceite 20W50 1L',    2, 20550.00, 41100.00);

-- Factura 11 — Jose, Honda Wave
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 1, 10, '2026-01-28 14:30:00', 40000.00, 104100.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (11, 'Freno delantero',    1, 36900.00, 36900.00),
    (11, 'Bujía',              1, 26000.00, 26000.00),
    (11, 'Filtro de aire',     1, 18900.00,  1200.00);

-- Factura 12 — Andrés, KTM Duke (cliente recurrente)
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 2, 4, '2026-01-28 09:30:00', 70000.00, 175300.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (12, 'Llanta trasera',     1, 85000.00, 85000.00),
    (12, 'Válvula llanta',     2,  4500.00,  9000.00),
    (12, 'Balanceo',           1, 11300.00, 11300.00);

-- Factura 13 — Andrés, Suzuki GN
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 2, 5, '2026-01-28 11:15:00', 45000.00, 145000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (13, 'Kit de arrastre',    1, 75000.00, 75000.00),
    (13, 'Piñón delantero',    1, 25000.00, 25000.00);

-- Factura 14 — Carlos, Yamaha FZ (cliente recurrente)
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 3, 2, '2026-01-28 10:00:00', 50000.00, 185000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (14, 'Pastillas de freno', 2,  8500.00, 17000.00),
    (14, 'Disco de freno',     1, 85000.00, 85000.00),
    (14, 'Líquido de frenos',  1, 33000.00, 33000.00);

-- Factura 15 — Carlos, TVS Apache
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 3, 7, '2026-01-28 13:00:00', 60000.00, 160000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (15, 'Carburador completo', 1, 95000.00, 95000.00),
    (15, 'Kit de carburador',   1,  5000.00,  5000.00);

-- Factura 16 — Julián, AKT 125
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 4, 6, '2026-01-28 09:15:00', 30000.00, 62000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (16, 'Cable del acelerador', 1, 19500.00, 19500.00),
    (16, 'Manguera gasolina',    1, 12500.00, 12500.00);

-- Factura 17 — Julián, Honda CB
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 4, 3, '2026-01-28 15:30:00', 55000.00, 130400.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (17, 'Freno delantero',    1, 36900.00, 36900.00),
    (17, 'Freno trasero',      1, 38500.00, 38500.00);

-- Factura 18 — Jose, Boxer 125
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 1, 8, '2026-01-28 16:00:00', 20000.00, 55000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (18, 'Bujía',              1, 26000.00, 26000.00),
    (18, 'Filtro de aire',     1,  9000.00,  9000.00);

-- Factura 19 — Andrés, Honda Wave
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 2, 10, '2026-01-28 16:45:00', 30000.00, 88000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (19, 'Aceite 20W50 1L',    2, 22500.00, 45000.00),
    (19, 'Filtro de aceite',   1, 13000.00, 13000.00);

-- Factura 20 — Carlos, AKT 125
INSERT INTO invoice (invoice_type, mechanic_id, vehicle_id, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('SERVICE', 3, 6, '2026-01-28 17:00:00', 25000.00, 67000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (20, 'Palanca de freno',   1, 18500.00, 18500.00),
    (20, 'Cable freno trasero',1, 23500.00, 23500.00);

-- -------------------------------------------------------------
-- FACTURAS DELIVERY
-- -------------------------------------------------------------

-- Factura 21
INSERT INTO invoice (invoice_type, buyer_name, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('DELIVERY', 'Talleres La 80', '2026-01-27 12:00:00', 0, 73000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (21, 'Suzuki 150 Palanca de freno', 2, 18500.00, 37000.00),
    (21, 'Boxer 125 Filtro de aire',    3, 12000.00, 36000.00);

-- Factura 22
INSERT INTO invoice (invoice_type, buyer_name, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('DELIVERY', 'Motocentro Bello', '2026-01-27 16:30:00', 0, 120000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (22, 'Honda CB Cable freno delantero', 4, 22500.00, 90000.00),
    (22, 'Honda CB Manija freno',          2, 15000.00, 30000.00);

-- Factura 23
INSERT INTO invoice (invoice_type, buyer_name, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('DELIVERY', 'Repuestos El Paisa', '2026-01-28 11:00:00', 0, 95000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (23, 'Yamaha FZ Filtro de aceite',    5, 18900.00, 94500.00),
    (23, 'Yamaha FZ Bujía NGK',           1,   500.00,    500.00);

-- Factura 24
INSERT INTO invoice (invoice_type, buyer_name, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('DELIVERY', 'Carlos Pérez', '2026-01-28 14:00:00', 0, 45000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (24, 'KTM Duke Pastillas de freno',   2, 22500.00, 45000.00);

-- Factura 25
INSERT INTO invoice (invoice_type, buyer_name, created_at, labor_amount, total_amount, is_cancelled)
VALUES ('DELIVERY', 'Talleres La 80', '2026-01-28 17:30:00', 0, 168000.00, false);

INSERT INTO invoice_item (invoice_id, description, quantity, unit_price, subtotal) VALUES
    (25, 'Boxer 150 Llanta delantera',    2, 72000.00, 144000.00),
    (25, 'Boxer 150 Válvula llanta',      4,  6000.00,  24000.00);

-- -------------------------------------------------------------
-- LIQUIDACIONES DIARIAS
-- Incluye solo facturas SERVICE activas (is_cancelled = false)
-- -------------------------------------------------------------

-- Día 2026-01-27
-- Jose:   facturas 1, 2 → total = 98900 + 149900 = 248800
INSERT INTO daily_liquidation (mechanic_id, date, total_revenue, mechanic_share, shop_share, invoice_count)
VALUES (1, '2026-01-27', 248800.00, 174160.00, 74640.00, 2);

-- Andrés: facturas 3, 4 → total = 209100 + 88500 = 297600
INSERT INTO daily_liquidation (mechanic_id, date, total_revenue, mechanic_share, shop_share, invoice_count)
VALUES (2, '2026-01-27', 297600.00, 208320.00, 89280.00, 2);

-- Carlos: facturas 5, 6 → total = 112000 + 74800 = 186800
INSERT INTO daily_liquidation (mechanic_id, date, total_revenue, mechanic_share, shop_share, invoice_count)
VALUES (3, '2026-01-27', 186900.00, 130830.00, 56070.00, 2);

-- Julián: factura 8 (factura 7 anulada) → total = 62900
INSERT INTO daily_liquidation (mechanic_id, date, total_revenue, mechanic_share, shop_share, invoice_count)
VALUES (4, '2026-01-27', 62900.00, 44030.00, 18870.00, 1);
