USE vim_3;

-- =====================================================
-- Script to add approval history column to store remarks in sequence
-- =====================================================

-- Check if column exists before adding
SET @col_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'vim_3'
      AND TABLE_NAME = 'cfg_tbl_custom_form_application'
      AND COLUMN_NAME = 'txt_approval_history'
);

-- Add column to store approval history as JSON array (only if it doesn't exist)
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE cfg_tbl_custom_form_application ADD COLUMN txt_approval_history JSON NULL COMMENT ''Stores approval history with remarks in sequence: [{"level": 1, "departmentId": 1, "departmentName": "Dept", "remarks": "text", "approvedBy": 1, "approvedDate": "timestamp"}]''',
    'SELECT ''Column txt_approval_history already exists'' AS Status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify column was added
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'vim_3'
  AND TABLE_NAME = 'cfg_tbl_custom_form_application'
  AND COLUMN_NAME = 'txt_approval_history';

SELECT 'Column txt_approval_history check completed!' AS Status;

