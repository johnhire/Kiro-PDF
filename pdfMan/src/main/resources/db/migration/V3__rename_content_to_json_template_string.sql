-- Flyway migration V3: rename template.content column to json_template_string
ALTER TABLE template RENAME COLUMN content TO json_template_string;
