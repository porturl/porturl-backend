-- This migration renames the application_category table to application_category_link
-- to match the new @ManyToMany mapping in Category.java.

ALTER TABLE application_category RENAME TO application_category_link;

-- If renaming doesn't update the index/constraint names in some DBs, 
-- we might want to drop and recreate, but for H2/Generic, RENAME usually works.
-- Hibernate's @OrderColumn will use the existing 'sort_order' column.
