-- Add new icon column
ALTER TABLE application ADD COLUMN icon VARCHAR(255);

-- Migrate data: use thumbnail if available, otherwise medium, otherwise large
UPDATE application 
SET icon = COALESCE(icon_thumbnail, icon_medium, icon_large);

-- Remove old columns
ALTER TABLE application DROP COLUMN icon_large;
ALTER TABLE application DROP COLUMN icon_medium;
ALTER TABLE application DROP COLUMN icon_thumbnail;
