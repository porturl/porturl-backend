-- Step 1: Create the new 'category' table with all its final columns.
CREATE TABLE category (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0,
    application_sort_mode VARCHAR(255) NOT NULL DEFAULT 'CUSTOM',
    icon VARCHAR(255),
    description VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Step 2: Create the join table for the many-to-many relationship.
CREATE TABLE application_categories (
    application_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (application_id, category_id),
    FOREIGN KEY (application_id) REFERENCES application(id),
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- Step 3: Add the sort_order column to the main application table.
ALTER TABLE application ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

-- Step 4 (Data Migration): Create new categories from the distinct old category strings.
-- This ensures that your existing application groupings are preserved.
INSERT INTO category (name)
SELECT DISTINCT category FROM application WHERE category IS NOT NULL;

-- Step 5 (Data Migration): Populate the new join table by linking each application
-- to the category that matches its old category string.
INSERT INTO application_categories (application_id, category_id)
SELECT a.id, c.id
FROM application a
JOIN category c ON a.category = c.name;

-- Step 6: Drop the old, redundant 'category' string column from the application table.
ALTER TABLE application DROP COLUMN category;
