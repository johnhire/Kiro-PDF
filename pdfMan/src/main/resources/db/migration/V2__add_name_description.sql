-- Flyway migration V2: add name and description columns to template and document tables.

ALTER TABLE template ADD COLUMN description VARCHAR(1000);

ALTER TABLE document ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE document ADD COLUMN description VARCHAR(1000);

-- Remove the default after adding the column
ALTER TABLE document ALTER COLUMN name DROP DEFAULT;
