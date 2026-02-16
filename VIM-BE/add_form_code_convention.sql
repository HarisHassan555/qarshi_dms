-- =====================================================
-- SQL Script: Add Form Code and Convention Support
-- Database: vim_3
-- Description: Adds columns for form code and convention tracking
-- =====================================================

USE vim_3;

-- Add form code column to store the generated code (e.g., CAPF-0001)
ALTER TABLE cfg_tbl_custom_form 
ADD COLUMN txt_form_code VARCHAR(50) NULL UNIQUE
AFTER txt_form_name;

-- Add convention prefix column to track the convention (e.g., CAPF, PRC)
ALTER TABLE cfg_tbl_custom_form 
ADD COLUMN txt_convention_prefix VARCHAR(50) NULL
AFTER txt_form_code;

-- Add index for faster lookups
CREATE INDEX idx_form_code ON cfg_tbl_custom_form(txt_form_code);
CREATE INDEX idx_convention_prefix ON cfg_tbl_custom_form(txt_convention_prefix);

-- Verify the columns were added
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form'
  AND COLUMN_NAME IN ('txt_form_code', 'txt_convention_prefix')
ORDER BY COLUMN_NAME;

SELECT 'Form code and convention columns added successfully!' AS Status;





