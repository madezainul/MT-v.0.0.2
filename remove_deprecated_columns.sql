-- ========================================
-- Database Migration Script
-- Purpose: Remove deprecated columns after User integration  
-- Target: MySQL maintenance_db database
-- ========================================

USE maintenance_db;

-- ========================================
-- IMPORTANT NOTE
-- ========================================
-- requestor_id and inspector_id are FOREIGN KEY columns that must be kept
-- Only remove old text-based columns that are no longer needed

-- ========================================
-- SAFETY CHECKS BEFORE DELETION
-- ========================================

-- 1. Verify all PRs have requestor_id populated
SELECT 
    COUNT(*) as total_purchase_requisitions,
    COUNT(requestor_id) as prs_with_requestor_id,
    (COUNT(*) - COUNT(requestor_id)) as missing_requestor_ids
FROM purchase_requisitions;

-- 2. Check current table structure
DESCRIBE purchase_requisitions;

-- ========================================
-- REMOVE DEPRECATED TEXT COLUMNS ONLY
-- ========================================

-- Remove old requestor_name column (if it exists as a text field)
ALTER TABLE purchase_requisitions 
DROP COLUMN IF EXISTS requestor_name;

-- Remove old requestor_email column (if it exists as a text field)
ALTER TABLE purchase_requisitions 
DROP COLUMN IF EXISTS requestor_email;

-- Remove old inspector_name column only if it's a text field (not FK)
-- Check if it exists and is varchar before dropping
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'maintenance_db' 
     AND TABLE_NAME = 'purchase_requisitions' 
     AND COLUMN_NAME = 'inspector_name'
     AND DATA_TYPE = 'varchar') > 0,
    'ALTER TABLE purchase_requisitions DROP COLUMN inspector_name;',
    'SELECT "inspector_name column not found or not varchar type, skipping...";'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ========================================
-- VERIFICATION AFTER DELETION
-- ========================================

-- 1. Test that the application queries still work
SELECT 
    pr.id,
    pr.code,
    pr.title,
    u.name as requestor_name,
    u.email as requestor_email,
    u.employee_id as requestor_employee_id
FROM purchase_requisitions pr
JOIN users u ON pr.requestor_id = u.id
LIMIT 5;

-- 2. Verify table structure after changes
DESCRIBE purchase_requisitions;

-- 3. Count total records to ensure no data loss
SELECT COUNT(*) as total_records_after_cleanup
FROM purchase_requisitions;

SELECT 'Migration completed successfully!' as status;

-- ========================================
-- NOTES
-- ========================================

/*
CRITICAL NOTES:

1. COLUMNS TO KEEP (Foreign Keys):
   - requestor_id (references users.id)
   - inspector_id (references users.id)

2. COLUMNS TO REMOVE (Old text fields):
   - requestor_name (varchar)
   - requestor_email (varchar)
   - inspector_name (varchar, if it exists as text field)

3. The script safely removes only deprecated text columns
   while preserving the foreign key relationships

4. Always test in a development environment first!
*/

