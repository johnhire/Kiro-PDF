-- Flyway migration V6: add html_template column to document table
ALTER TABLE document ADD COLUMN html_template TEXT;
