-- Flyway migration V1: initial schema for pdfMan
-- Creates the template and document tables with full audit trail columns.

CREATE TABLE template (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255)
);

CREATE TABLE document (
    id          BIGSERIAL    PRIMARY KEY,
    template_id BIGINT       NOT NULL REFERENCES template(id),
    storage_key VARCHAR(512) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ,
    created_by  VARCHAR(255) NOT NULL,
    updated_by  VARCHAR(255)
);

CREATE INDEX idx_document_template_id ON document(template_id);
