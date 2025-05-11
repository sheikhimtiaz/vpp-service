-- 001-create-battery-table.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS battery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    postcode VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    CONSTRAINT uq_battery_name_postcode UNIQUE (name, postcode)
);

-- Indexes
CREATE INDEX idx_battery_name ON battery (name);
CREATE INDEX idx_battery_postcode ON battery (postcode);
