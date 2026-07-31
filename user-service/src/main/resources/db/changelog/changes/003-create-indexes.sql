--liquibase formatted sql

--changeset pavel-kupreichik:003-create-indexes
CREATE INDEX idx_payment_cards_user_id
    ON payment_cards (user_id);

CREATE INDEX idx_payment_cards_number
    ON payment_cards (number);

--rollback DROP INDEX idx_payment_cards_number;
--rollback DROP INDEX idx_payment_cards_user_id;
