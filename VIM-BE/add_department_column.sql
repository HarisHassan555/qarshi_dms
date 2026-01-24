-- =====================================================
-- SQL Script: Add Department Column to User Table
-- Database: vim_3
-- Description: Adds ser_department_id foreign key column to cfg_tbl_user
-- =====================================================

USE vim_3;

-- Check if column already exists
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'vim_3' 
    AND TABLE_NAME = 'cfg_tbl_user' 
    AND COLUMN_NAME = 'ser_department_id'
);

-- Add column if it doesn't exist
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE cfg_tbl_user 
     ADD COLUMN ser_department_id INT NULL 
     COMMENT ''Foreign key to hr_tbl_department''',
    'SELECT "Column ser_department_id already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add foreign key constraint if column was just created
SET @fk_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
    WHERE TABLE_SCHEMA = 'vim_3' 
    AND TABLE_NAME = 'cfg_tbl_user' 
    AND CONSTRAINT_NAME = 'fk_user_department'
);

SET @sql_fk = IF(@fk_exists = 0 AND @column_exists = 0,
    'ALTER TABLE cfg_tbl_user 
     ADD CONSTRAINT fk_user_department 
     FOREIGN KEY (ser_department_id) 
     REFERENCES hr_tbl_department(ser_department_id) 
     ON DELETE SET NULL 
     ON UPDATE CASCADE',
    'SELECT "Foreign key constraint already exists or column was not created" AS message'
);

PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

-- Verify the column was added
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_user'
  AND COLUMN_NAME = 'ser_department_id';

