USE velocity;

SET @db = DATABASE();
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'cfg_tbl_user' AND COLUMN_NAME = 'txt_profile_picture_path') = 0,
    'ALTER TABLE cfg_tbl_user ADD COLUMN txt_profile_picture_path VARCHAR(512) NULL AFTER txt_signature_path',
    'SELECT ''txt_profile_picture_path already exists'' AS msg'
);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
