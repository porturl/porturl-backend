-- Fix the primary key of application_category_link to allow Hibernate @OrderColumn to reorder apps.
-- The current PK (application_id, category_id) prevents updating the application_id for a given sort_order 
-- if that application_id already exists in another row of the same category during the update process.

ALTER TABLE application_category_link DROP PRIMARY KEY;
ALTER TABLE application_category_link ADD PRIMARY KEY (category_id, sort_order);

-- Optional: Add a unique constraint to ensure an application isn't added to the same category twice.
-- This replaces the original uniqueness provided by the old PK.
ALTER TABLE application_category_link ADD CONSTRAINT unique_app_category UNIQUE (category_id, application_id);
