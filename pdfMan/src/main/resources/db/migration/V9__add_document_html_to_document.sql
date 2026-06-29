-- Flyway migration V9: add document_html column to document table
ALTER TABLE document ADD COLUMN document_html TEXT;
