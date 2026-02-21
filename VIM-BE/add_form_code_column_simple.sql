-- =====================================================
-- SQL Script: Add Form Code Column to Application Table (Simple Version)
-- Database: vim_3
-- Description: Adds txt_form_code column to cfg_tbl_custom_form_application
-- =====================================================

USE vim_3;

-- Check if column exists first
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN 'Column txt_form_code already exists'
        ELSE 'Column txt_form_code does NOT exist - will be added'
    END AS 'Column Status'
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_application'
  AND COLUMN_NAME = 'txt_form_code';

-- Add the column (will fail gracefully if it already exists)
ALTER TABLE cfg_tbl_custom_form_application
ADD COLUMN txt_form_code VARCHAR(50) NULL COMMENT 'Form code from the form template (e.g., CAPF-0001)'
AFTER ser_form_id;

-- Verify the column was added
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_application'
  AND COLUMN_NAME = 'txt_form_code';

SELECT 'Form code column added successfully!' AS Status;









