-- V1: Portal users for the dummy college portal
CREATE TABLE portal_users (
    id CHAR(36) NOT NULL,
    staff_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;
