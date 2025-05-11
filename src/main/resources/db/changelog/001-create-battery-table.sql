-- 001-create-battery-table.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS battery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    postcode VARCHAR(20) NOT NULL,
    capacity INT NOT NULL
);

-- Indexes
CREATE INDEX idx_battery_postcode ON battery (postcode);
CREATE INDEX idx_battery_capacity ON battery (capacity);
