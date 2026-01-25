-- =====================================================
-- SQL Script: Fix Approval Pipeline Table AUTO_INCREMENT
-- Database: vim_3
-- Description: Ensures ser_approval_pipeline_id has AUTO_INCREMENT enabled
-- =====================================================

USE vim_3;

-- Check current table structure
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_approval_pipeline'
  AND COLUMN_NAME LIKE '%pipeline_id%';

-- Fix: Ensure ser_approval_pipeline_id has AUTO_INCREMENT
-- This will modify the column to have AUTO_INCREMENT if it doesn't already
ALTER TABLE cfg_tbl_custom_form_approval_pipeline 
MODIFY COLUMN ser_approval_pipeline_id INT AUTO_INCREMENT;

-- Verify the fix
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

SELECT 'AUTO_INCREMENT fix completed' AS Status;

