-- Purchase Requisition Table Cleanup Script - MySQL
-- Database: maintenance_db
-- Remove requestor_name and requestor_email columns after User integration
-- Run this script after confirming the application works correctly with User relationship

-- ========================================
-- SELECT DATABASE FIRST
-- ========================================

USE maintenance_db;

-- ========================================
-- SAFETY CHECKS BEFORE DELETION
-- ========================================

-- 1. Verify all PRs have requestor_id populated
SELECT 
    COUNT(*) as total_purchase_requisitions,
    COUNT(requestor_id) as prs_with_requestor_id,
    (COUNT(*) - COUNT(requestor_id)) as missing_requestor_ids
FROM purchase_requisitions;

-- 2. Check for any PRs without requestor_id (should be 0)
SELECT COUNT(*) as prs_without_requestor_id
FROM purchase_requisitions 
WHERE requestor_id IS NULL;

-- 3. Verify requestor_id references valid users
SELECT 
    pr.id as pr_id,
    pr.code as pr_code,
    pr.requestor_id,
    u.name as user_name
FROM purchase_requisitions pr
LEFT JOIN users u ON pr.requestor_id = u.id
WHERE u.id IS NULL;

-- 4. Check current table structure
DESCRIBE purchase_requisitions;

-- ========================================
-- REMOVE THE COLUMNS (MySQL Syntax)
-- ========================================

-- Remove requestor_name column
ALTER TABLE purchase_requisitions 
DROP COLUMN requestor_name;

-- Remove requestor_email column  
ALTER TABLE purchase_requisitions 
DROP COLUMN requestor_email;

ALTER TABLE purchase_requisitions   
DROP COLUMN requestor_id;

ALTER TABLE purchase_requisitions   
DROP COLUMN inspector_name;

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

-- ========================================
-- NOTES
-- ========================================

/*
IMPORTANT NOTES:

1. DATABASE: This script is designed for MySQL database: maintenance_db

2. PREREQUISITES: 
   - Ensure your application works correctly with the User relationship
   - All PRs should have valid requestor_id values
   - All safety checks above should return expected results

3. EXECUTION:
   - Run safety checks first
   - Execute the ALTER TABLE statements during low-usage time
   - Verify with test queries

4. NO ROLLBACK: Since you don't want backup, make sure you're confident
   about the changes before running the DROP COLUMN statements

5. TIMING: Run this during a maintenance window when the application 
   is not heavily used
*/