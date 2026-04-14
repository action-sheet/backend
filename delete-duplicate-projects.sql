-- SQL Script to View and Delete Duplicate Projects
-- Run this in H2 Console: http://localhost:8080/h2-console

-- Step 1: View all projects to identify duplicates
SELECT id, name, created_date, active 
FROM projects 
ORDER BY name, created_date;

-- Step 2: Delete specific duplicate projects
-- IMPORTANT: Review the list above first, then uncomment and run the DELETE statements below

-- Delete "Default Project" (if it's a duplicate/unwanted)
-- DELETE FROM projects WHERE name = 'Default Project';

-- Delete "Mana" (if it's a duplicate)
-- DELETE FROM projects WHERE name = 'Mana';

-- Delete "Mercedes-Shuwaikh" duplicates (keep only one, delete others by ID)
-- First, see which Mercedes-Shuwaikh entries exist:
-- SELECT id, name, created_date FROM projects WHERE name LIKE '%Mercedes%Shuwaikh%';

-- Then delete by specific ID (replace 'PROJECT_ID_HERE' with actual ID):
-- DELETE FROM projects WHERE id = 'PROJECT_ID_HERE';

-- Step 3: Verify deletion
-- SELECT id, name FROM projects ORDER BY name;
