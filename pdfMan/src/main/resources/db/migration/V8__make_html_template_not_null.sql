-- Flyway migration V8: make html_template NOT NULL on template table
-- Assumes all existing rows have been updated with a non-null value
ALTER TABLE template ALTER COLUMN html_template SET NOT NULL;
