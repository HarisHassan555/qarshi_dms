USE vim_3;

-- =====================================================
-- Script to add department head column to hr_tbl_department
-- =====================================================

-- Step 1: Check if column already exists
SELECT 
    'Checking for ser_department_head_id column...' AS Status;

SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'hr_tbl_department'
  AND COLUMN_NAME = 'ser_department_head_id';

-- Step 2: Add column if it doesn't exist
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'hr_tbl_department'
      AND COLUMN_NAME = 'ser_department_head_id'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE hr_tbl_department 
     ADD COLUMN ser_department_head_id INT NULL 
     COMMENT "Foreign key to cfg_tbl_user - Department Head User ID"
     AFTER ser_parent_department_id',
    'SELECT "Column ser_department_head_id already exists" AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 
    CASE 
        WHEN @column_exists = 0 THEN 'Column ser_department_head_id added successfully'
        ELSE 'Column ser_department_head_id already exists'
    END AS 'Column Status';

-- Step 3: Add foreign key constraint if column was just created
SET @fk_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'hr_tbl_department'
      AND CONSTRAINT_NAME = 'fk_department_head_user'
);

SET @sql_fk = IF(@fk_exists = 0 AND @column_exists = 0,
    'ALTER TABLE hr_tbl_department 
     ADD CONSTRAINT fk_department_head_user 
     FOREIGN KEY (ser_department_head_id) 
     REFERENCES cfg_tbl_user(ser_user_id) 
     ON DELETE SET NULL 
     ON UPDATE CASCADE',
    'SELECT "Foreign key constraint already exists or column was not created" AS message'
);

PREPARE stmt_fk FROM @sql_fk;
EXECUTE stmt_fk;
DEALLOCATE PREPARE stmt_fk;

SELECT 
    CASE 
        WHEN @fk_exists = 0 AND @column_exists = 0 THEN 'Foreign key constraint added successfully'
        WHEN @fk_exists > 0 THEN 'Foreign key constraint already exists'
        ELSE 'Foreign key constraint not added (column already existed)'
    END AS 'FK Status';

-- Step 4: Verification
SELECT 
    'Verification: Department Head Column' AS 'Check';
    
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_KEY,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'hr_tbl_department'
  AND COLUMN_NAME = 'ser_department_head_id';

SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'hr_tbl_department'
  AND CONSTRAINT_NAME = 'fk_department_head_user';

SELECT 
    'FINAL STATUS' AS 'Check',
    CASE 
        WHEN EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = 'vim_3'
              AND TABLE_NAME = 'hr_tbl_department'
              AND COLUMN_NAME = 'ser_department_head_id'
        ) THEN 'SUCCESS - Department head column added to hr_tbl_department table'
        ELSE 'ERROR - Column was not added'
    END AS 'Status';

