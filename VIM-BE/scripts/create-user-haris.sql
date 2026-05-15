-- Create app user: haris / 123 (BCrypt) in database velocity
-- Run entire script in MySQL Workbench

USE velocity;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @password_hash = '$2b$10$jx2bjar6ZLmcnrZyDlKa2OLBzZjMABAEXNWi.Zo52.QurLztCNj7K';

-- ---------- diagnostics ----------
SELECT 'cfg_tbl_role count' AS info, COUNT(*) AS cnt FROM cfg_tbl_role;
SELECT 'cfg_tbl_user count' AS info, COUNT(*) AS cnt FROM cfg_tbl_user;
SELECT ser_role_id, txt_role_name, bl_is_deleted, bln_status FROM cfg_tbl_role LIMIT 15;

-- ---------- ensure at least one ADMIN role exists ----------
SET @admin_role_id = (
    SELECT ser_role_id FROM cfg_tbl_role
    WHERE (txt_role_name LIKE '%ADMIN%' OR txt_role_name LIKE '%admin%' OR UPPER(txt_role_name) = 'ADMIN')
    ORDER BY ser_role_id LIMIT 1
);

SET @admin_role_id = IFNULL(@admin_role_id, (
    SELECT ser_role_id FROM cfg_tbl_role ORDER BY ser_role_id LIMIT 1
));

SET @next_role_id = IFNULL((SELECT MAX(ser_role_id) + 1 FROM cfg_tbl_role), 1);

INSERT INTO cfg_tbl_role (
    ser_role_id, txt_role_name, txt_role_code,
    bl_is_active, bln_status, bl_is_deleted,
    dte_created_date, ser_created_user
)
SELECT @next_role_id, 'ADMIN', 'ADMIN', 1, 1, 0, NOW(), 1
WHERE @admin_role_id IS NULL;

SET @admin_role_id = IFNULL(@admin_role_id, @next_role_id);
SET @role_name = (SELECT txt_role_name FROM cfg_tbl_role WHERE ser_role_id = @admin_role_id LIMIT 1);

SELECT @admin_role_id AS 'Role ID', @role_name AS 'Role Name';

-- ---------- create or update user haris ----------
SET @existing_user_id = (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE LOWER(TRIM(txt_user_name)) = 'haris'
    LIMIT 1
);

SET @next_user_id = IFNULL((SELECT MAX(ser_user_id) + 1 FROM cfg_tbl_user), 1);

INSERT INTO cfg_tbl_user (
    ser_user_id, txt_user_name, txt_password, ser_role_id, txt_role,
    bl_is_active, bln_status, bl_is_deleted, dte_created_date, ser_created_user
)
SELECT @next_user_id, 'haris', @password_hash, @admin_role_id, IFNULL(@role_name, 'ADMIN'),
       1, 1, 0, NOW(), 1
WHERE @existing_user_id IS NULL;

SET @haris_user_id = IFNULL(@existing_user_id, @next_user_id);

UPDATE cfg_tbl_user
SET txt_user_name = 'haris',
    txt_password = @password_hash,
    ser_role_id = @admin_role_id,
    txt_role = IFNULL(@role_name, 'ADMIN'),
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE ser_user_id = @haris_user_id;

-- Re-read id in case name matched a different row
SET @haris_user_id = (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE LOWER(TRIM(txt_user_name)) = 'haris' LIMIT 1
);

SELECT @haris_user_id AS 'Haris User ID';

-- ---------- menu permissions (skip if no submenus yet) ----------
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id, ser_role_id, ser_user_id,
    bl_is_active, bln_status, bl_is_deleted,
    bl_is_view, bl_is_add, bl_is_delete, bl_is_update,
    bl_is_approve, bl_is_enabled, bl_is_all,
    dte_created_date, ser_created_user
)
SELECT sm.ser_sub_menu_id, @admin_role_id, @haris_user_id,
       1, 1, 0, 1, 1, 1, 1, 1, 1, 1, NOW(), 1
FROM cfg_tbl_sub_menu sm
WHERE @haris_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
      AND smr.ser_user_id = @haris_user_id
  );

SELECT ser_user_id, txt_user_name, txt_role, ser_role_id,
       LEFT(txt_password, 7) AS pwd_prefix, bl_is_active, bln_status, bl_is_deleted
FROM cfg_tbl_user
WHERE ser_user_id = @haris_user_id;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;
