-- =====================================================
-- SQL Script: Fix All Custom Form Tables AUTO_INCREMENT
-- Database: vim_3
-- Description: Ensures all primary key columns have AUTO_INCREMENT enabled
-- =====================================================

USE vim_3;

-- =====================================================
-- 1. Fix cfg_tbl_custom_form table
-- =====================================================
SELECT 'Fixing cfg_tbl_custom_form...' AS Status;

-- Check current structure
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form'
  AND COLUMN_NAME = 'ser_form_id';

-- Fix: Ensure ser_form_id has AUTO_INCREMENT
ALTER TABLE cfg_tbl_custom_form 
MODIFY COLUMN ser_form_id INT AUTO_INCREMENT;

-- =====================================================
-- 2. Fix cfg_tbl_custom_form_field table
-- =====================================================
SELECT 'Fixing cfg_tbl_custom_form_field...' AS Status;

-- Check current structure
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_field'
  AND COLUMN_NAME = 'ser_field_id';

-- Fix: Ensure ser_field_id has AUTO_INCREMENT
ALTER TABLE cfg_tbl_custom_form_field 
MODIFY COLUMN ser_field_id INT AUTO_INCREMENT;

-- =====================================================
-- 3. Fix cfg_tbl_custom_form_approval_pipeline table
-- =====================================================
SELECT 'Fixing cfg_tbl_custom_form_approval_pipeline...' AS Status;

-- Check current structure
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_approval_pipeline'
  AND COLUMN_NAME = 'ser_approval_pipeline_id';

-- Fix: Ensure ser_approval_pipeline_id has AUTO_INCREMENT
ALTER TABLE cfg_tbl_custom_form_approval_pipeline 
MODIFY COLUMN ser_approval_pipeline_id INT AUTO_INCREMENT;

-- =====================================================
-- 4. Verify all fixes
-- =====================================================
SELECT 'Verifying all fixes...' AS Status;

SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME IN (
    'cfg_tbl_custom_form',
    'cfg_tbl_custom_form_field',
    'cfg_tbl_custom_form_approval_pipeline'
  )
  AND COLUMN_KEY = 'PRI'
ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT 'All fixes completed successfully!' AS Status;



