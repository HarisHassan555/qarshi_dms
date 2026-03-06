-- =====================================================
-- SQL Script: Fix ser_form_id AUTO_INCREMENT (with FK handling)
-- Database: vim_3
-- Description: Enables AUTO_INCREMENT on ser_form_id column
--              by temporarily dropping and recreating foreign key constraints
-- =====================================================

USE vim_3;

-- =====================================================
-- Step 1: Find and drop foreign key constraints
-- =====================================================
SELECT 'Step 1: Finding foreign key constraints...' AS Status;

-- Find all foreign keys that reference ser_form_id
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'vim_3'
  AND REFERENCED_TABLE_NAME = 'cfg_tbl_custom_form'
  AND REFERENCED_COLUMN_NAME = 'ser_form_id';

-- Drop foreign key from cfg_tbl_custom_form_field
SET @fk_field = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_custom_form_field'
      AND REFERENCED_TABLE_NAME = 'cfg_tbl_custom_form'
      AND REFERENCED_COLUMN_NAME = 'ser_form_id'
    LIMIT 1
);

SET @fk_pipeline = (
    SELECT CONSTRAINT_NAME
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_custom_form_approval_pipeline'
      AND REFERENCED_TABLE_NAME = 'cfg_tbl_custom_form'
      AND REFERENCED_COLUMN_NAME = 'ser_form_id'
    LIMIT 1
);

-- Drop foreign keys if they exist
SET @sql1 = IF(@fk_field IS NOT NULL, 
    CONCAT('ALTER TABLE cfg_tbl_custom_form_field DROP FOREIGN KEY ', @fk_field), 
    'SELECT "No FK on cfg_tbl_custom_form_field" AS message');
PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

SET @sql2 = IF(@fk_pipeline IS NOT NULL, 
    CONCAT('ALTER TABLE cfg_tbl_custom_form_approval_pipeline DROP FOREIGN KEY ', @fk_pipeline), 
    'SELECT "No FK on cfg_tbl_custom_form_approval_pipeline" AS message');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- =====================================================
-- Step 2: Modify ser_form_id to add AUTO_INCREMENT
-- =====================================================
SELECT 'Step 2: Adding AUTO_INCREMENT to ser_form_id...' AS Status;

ALTER TABLE cfg_tbl_custom_form 
MODIFY COLUMN ser_form_id INT AUTO_INCREMENT;

-- =====================================================
-- Step 3: Recreate foreign key constraints
-- =====================================================
SELECT 'Step 3: Recreating foreign key constraints...' AS Status;

-- Recreate FK for cfg_tbl_custom_form_field
ALTER TABLE cfg_tbl_custom_form_field
ADD CONSTRAINT fk_custom_form_field_form
FOREIGN KEY (ser_form_id) 
REFERENCES cfg_tbl_custom_form(ser_form_id) 
ON DELETE CASCADE;

-- Recreate FK for cfg_tbl_custom_form_approval_pipeline
ALTER TABLE cfg_tbl_custom_form_approval_pipeline
ADD CONSTRAINT fk_custom_form_pipeline_form
FOREIGN KEY (ser_form_id) 
REFERENCES cfg_tbl_custom_form(ser_form_id) 
ON DELETE CASCADE;

-- =====================================================
-- Step 4: Verify the fix
-- =====================================================
SELECT 'Step 4: Verifying AUTO_INCREMENT...' AS Status;

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

SELECT 'AUTO_INCREMENT enabled on ser_form_id successfully!' AS Status;







