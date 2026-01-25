-- =====================================================
-- SQL Script: Add Form Code Column to Application Table
-- Database: vim_3
-- Description: Adds txt_form_code column to cfg_tbl_custom_form_application
-- =====================================================

USE vim_3;

-- Check if the column already exists
SELECT 'Checking for txt_form_code column in cfg_tbl_custom_form_application...' AS Status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_application'
  AND COLUMN_NAME = 'txt_form_code';

-- Add the form code column if it does not exist
-- Using a stored procedure approach that works in MySQL
DROP PROCEDURE IF EXISTS AddFormCodeColumn;

DELIMITER //

CREATE PROCEDURE AddFormCodeColumn()
BEGIN
    DECLARE column_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO column_exists
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_custom_form_application'
      AND COLUMN_NAME = 'txt_form_code';
    
    IF column_exists = 0 THEN
        SELECT 'Adding txt_form_code column to cfg_tbl_custom_form_application...' AS Status;
        ALTER TABLE cfg_tbl_custom_form_application
        ADD COLUMN txt_form_code VARCHAR(50) NULL COMMENT 'Form code from the form template (e.g., CAPF-0001)'
        AFTER ser_form_id;
        SELECT 'txt_form_code column added successfully.' AS Status;
    ELSE
        SELECT 'txt_form_code column already exists.' AS Status;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL AddFormCodeColumn();

-- Clean up
DROP PROCEDURE IF EXISTS AddFormCodeColumn;

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

SELECT 'Form code column verification complete!' AS Status;

