-- =====================================================
-- SQL Script: Add Approval Pipeline JSON Column
-- Database: vim_3
-- Description: Adds JSON column to store approval pipeline as array
-- =====================================================

USE vim_3;

-- Add JSON column to store approval pipeline
ALTER TABLE cfg_tbl_custom_form 
ADD COLUMN txt_approval_pipeline JSON NULL 
AFTER txt_form_description;

-- Verify the column was added
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form'
  AND COLUMN_NAME = 'txt_approval_pipeline';

SELECT 'JSON column added successfully!' AS Status;

-- Note: The separate cfg_tbl_custom_form_approval_pipeline table can be kept
-- for backward compatibility or dropped later if not needed









