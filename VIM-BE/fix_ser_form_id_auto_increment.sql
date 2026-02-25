-- =====================================================
-- SQL Script: Fix ser_form_id AUTO_INCREMENT
-- Database: vim_3
-- Description: Enables AUTO_INCREMENT on ser_form_id column
-- =====================================================

USE vim_3;

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

-- Fix: Enable AUTO_INCREMENT on ser_form_id
ALTER TABLE cfg_tbl_custom_form 
MODIFY COLUMN ser_form_id INT AUTO_INCREMENT;

-- Verify the fix
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

SELECT 'AUTO_INCREMENT enabled on ser_form_id' AS Status;






