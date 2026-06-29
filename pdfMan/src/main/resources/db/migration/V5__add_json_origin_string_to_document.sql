-- Flyway migration V5: add json_origin_string column to document table
ALTER TABLE document ADD COLUMN json_origin_string TEXT;
