-- This migration refactors the schema to support per-category sorting for applications
-- by introducing a custom join table with a sort_order column.

-- Step 1: Create the new join table with the sort_order column.
CREATE TABLE application_category (
    application_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (application_id, category_id),
    FOREIGN KEY (application_id) REFERENCES application(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

-- Step 2 (Data Migration): Populate the new join table with existing relationships.
-- A default sort order of 0 is assigned to all existing links.
INSERT INTO application_category (application_id, category_id, sort_order)
SELECT application_id, category_id, 0 FROM application_categories;

-- Step 3: Drop the old, simple join table as it has been replaced.
DROP TABLE application_categories;

-- Step 4: Drop the now-redundant global sort_order from the application table.
ALTER TABLE application DROP COLUMN sort_order;
