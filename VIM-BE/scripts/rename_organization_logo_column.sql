-- Rename txt_logo_path → logo and place logo first (after primary key)
USE velocity;

SET @has_logo_path = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_tbl_organization' AND COLUMN_NAME = 'txt_logo_path'
);
SET @has_logo = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_tbl_organization' AND COLUMN_NAME = 'logo'
);

SET @sql_rename = IF(@has_logo_path > 0 AND @has_logo = 0,
    'ALTER TABLE hr_tbl_organization CHANGE COLUMN txt_logo_path logo VARCHAR(512) NULL AFTER ser_organization_id',
    'SELECT ''logo column already migrated or txt_logo_path missing'' AS message');
PREPARE stmt FROM @sql_rename;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_logo_after = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_tbl_organization' AND COLUMN_NAME = 'logo'
);
SET @sql_move = IF(@has_logo_after > 0,
    'ALTER TABLE hr_tbl_organization MODIFY COLUMN logo VARCHAR(512) NULL AFTER ser_organization_id',
    'SELECT ''logo column not found'' AS message');
PREPARE stmt FROM @sql_move;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
