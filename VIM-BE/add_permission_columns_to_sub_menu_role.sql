USE vim_3;

-- =====================================================
-- Script to add missing permission columns to cfg_tbl_sub_menu_role
-- These columns are required for canAdd and canUpdate checks
-- =====================================================

-- Step 1: Check if columns exist
SELECT 
    'Checking for permission columns...' AS Status;

SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_sub_menu_role'
  AND COLUMN_NAME IN ('bl_is_create', 'bl_is_NewView', 'bl_is_NewUpdate')
ORDER BY COLUMN_NAME;

-- Step 2: Add bl_is_create column if it doesn't exist
SET @column_exists_create = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_sub_menu_role'
      AND COLUMN_NAME = 'bl_is_create'
);

SET @sql_create = IF(@column_exists_create = 0,
    'ALTER TABLE cfg_tbl_sub_menu_role ADD COLUMN bl_is_create BOOLEAN NULL DEFAULT 0 COMMENT "blIsNewCreate - Required for canAdd check" AFTER bl_is_enabled',
    'SELECT "Column bl_is_create already exists" AS message'
);

PREPARE stmt_create FROM @sql_create;
EXECUTE stmt_create;
DEALLOCATE PREPARE stmt_create;

SELECT 
    CASE 
        WHEN @column_exists_create = 0 THEN 'Column bl_is_create added successfully'
        ELSE 'Column bl_is_create already exists'
    END AS 'bl_is_create Status';

-- Step 3: Add bl_is_NewView column if it doesn't exist
SET @column_exists_newview = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_sub_menu_role'
      AND COLUMN_NAME = 'bl_is_NewView'
);

SET @sql_newview = IF(@column_exists_newview = 0,
    'ALTER TABLE cfg_tbl_sub_menu_role ADD COLUMN bl_is_NewView BOOLEAN NULL DEFAULT 0 COMMENT "blIsNewView" AFTER bl_is_create',
    'SELECT "Column bl_is_NewView already exists" AS message'
);

PREPARE stmt_newview FROM @sql_newview;
EXECUTE stmt_newview;
DEALLOCATE PREPARE stmt_newview;

SELECT 
    CASE 
        WHEN @column_exists_newview = 0 THEN 'Column bl_is_NewView added successfully'
        ELSE 'Column bl_is_NewView already exists'
    END AS 'bl_is_NewView Status';

-- Step 4: Add bl_is_NewUpdate column if it doesn't exist
SET @column_exists_newupdate = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_sub_menu_role'
      AND COLUMN_NAME = 'bl_is_NewUpdate'
);

SET @sql_newupdate = IF(@column_exists_newupdate = 0,
    'ALTER TABLE cfg_tbl_sub_menu_role ADD COLUMN bl_is_NewUpdate BOOLEAN NULL DEFAULT 0 COMMENT "blIsNewUpdate - Required for canUpdate check" AFTER bl_is_NewView',
    'SELECT "Column bl_is_NewUpdate already exists" AS message'
);

PREPARE stmt_newupdate FROM @sql_newupdate;
EXECUTE stmt_newupdate;
DEALLOCATE PREPARE stmt_newupdate;

SELECT 
    CASE 
        WHEN @column_exists_newupdate = 0 THEN 'Column bl_is_NewUpdate added successfully'
        ELSE 'Column bl_is_NewUpdate already exists'
    END AS 'bl_is_NewUpdate Status';

-- Step 5: Verification
SELECT 
    'Verification: All permission columns' AS 'Check';
    
SELECT 
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_sub_menu_role'
  AND COLUMN_NAME IN ('bl_is_create', 'bl_is_NewView', 'bl_is_NewUpdate')
ORDER BY COLUMN_NAME;

SELECT 
    'FINAL STATUS' AS 'Check',
    CASE 
        WHEN EXISTS (
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = 'vim_3'
              AND TABLE_NAME = 'cfg_tbl_sub_menu_role'
              AND COLUMN_NAME IN ('bl_is_create', 'bl_is_NewView', 'bl_is_NewUpdate')
        ) THEN 'SUCCESS - All permission columns exist in cfg_tbl_sub_menu_role table'
        ELSE 'ERROR - Some columns are missing'
    END AS 'Status';




