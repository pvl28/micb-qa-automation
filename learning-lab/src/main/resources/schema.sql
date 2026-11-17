-- Synthetic learning schema. Not related to any real bank database.
CREATE TABLE IF NOT EXISTS rate_snapshot (
    message_id   UUID          PRIMARY KEY,          -- idempotency key: one row per message
    rate_date    DATE          NOT NULL,
    currency     CHAR(3)       NOT NULL,
    buy          NUMERIC(10,4) NOT NULL CHECK (buy > 0),
    sell         NUMERIC(10,4) NOT NULL CHECK (sell > 0),
    received_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT buy_not_above_sell CHECK (buy <= sell),
    CONSTRAINT one_rate_per_day UNIQUE (rate_date, currency)
);
