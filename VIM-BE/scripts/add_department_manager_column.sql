-- Add department manager user id(s) to hr_tbl_department (comma-separated cfg_tbl_user.ser_user_id values).
-- Database: velocity (change USE if needed)

USE velocity;

SET @db = DATABASE();

SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'hr_tbl_department' AND COLUMN_NAME = 'ser_department_manager_id') = 0,
    'ALTER TABLE hr_tbl_department ADD COLUMN ser_department_manager_id VARCHAR(255) NULL COMMENT ''Comma-separated cfg_tbl_user ids - department managers'' AFTER ser_department_head_id',
    'SELECT ''ser_department_manager_id exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = @db
  AND TABLE_NAME = 'hr_tbl_department'
  AND COLUMN_NAME IN ('ser_department_head_id', 'ser_department_manager_id');
