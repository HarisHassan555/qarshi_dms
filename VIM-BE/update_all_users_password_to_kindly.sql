USE velocity;

-- Reset all active users to password: kindly (BCrypt cost=10)
-- Generated for Spring BCryptPasswordEncoder compatibility ($2b$)

SET @new_password_hash = '$2b$10$5.D/B4jHp.LmZq5dMxCSNOSj4XguAdyPoho1JmZfwyTMPIxZW0c12';

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE cfg_tbl_user
SET
    txt_password = @new_password_hash,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
  AND @new_password_hash IS NOT NULL;

SELECT ROW_COUNT() AS users_updated;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

SELECT ser_user_id, txt_user_name, LEFT(txt_password, 7) AS bcrypt_prefix, dte_modified_date
FROM cfg_tbl_user
WHERE bl_is_deleted = 0 OR bl_is_deleted IS NULL
ORDER BY ser_user_id
LIMIT 20;
