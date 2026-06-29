-- Flyway migration V7: move html_template from document to template table
ALTER TABLE document DROP COLUMN IF EXISTS html_template;
ALTER TABLE template ADD COLUMN IF NOT EXISTS html_template TEXT;
