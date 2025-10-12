-- Database Migration: Change PO (Purchase Order) to Quotation Request
-- Execute these SQL statements in order

-- Phase 1: Drop Existing Tables
-- =============================

USE maintenance_db;

-- 1. Drop purchase_order_parts table first (due to foreign key dependency)
DROP TABLE IF EXISTS purchase_order_parts;

-- 2. Drop purchase_orders table  
DROP TABLE IF EXISTS purchase_orders;

-- Phase 2: Update purchase_requisition_parts table
-- ===============================================

-- 3. In purchase_requisition_parts table, rename po_number to quotation_number
ALTER TABLE purchase_requisition_parts RENAME COLUMN po_number TO quotation_number;

-- Phase 3: Create New Tables for Quotation Requests
-- =================================================

-- Note: After dropping the tables, you'll need to:
-- 1. Update your Java entities to use the new names
-- 2. Run your application to let JPA/Hibernate recreate the tables with new names
-- 3. The new tables will be created as:
--    - quotation_requests (instead of purchase_orders)
--    - quotation_request_parts (instead of purchase_order_parts)

-- Verification Queries
-- ===================

-- After migration, run these to verify:
-- SHOW TABLES; -- to confirm old tables are dropped
-- DESCRIBE purchase_requisition_parts; -- to confirm column rename
-- SELECT COUNT(*) FROM quotation_requests; -- after JPA recreates tables
-- SELECT COUNT(*) FROM quotation_request_parts; -- after JPA recreates tables