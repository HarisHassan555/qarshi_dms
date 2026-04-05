USE vim_3;

-- =====================================================
-- Add "Role Management" submenu under "User Management"
-- URL: roles
-- Grants permission only to ADMIN roles
-- =====================================================

-- 1) Find User Management menu id
SET @user_management_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'User Management'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
      AND (bln_status = 1 OR bln_status IS NULL)
    LIMIT 1
);

-- 2) Find existing submenu id (if already created)
SET @role_mgmt_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @user_management_menu_id
      AND txt_sub_menu_name IN ('Role Management', 'Role Managment')
    LIMIT 1
);

-- 3) Next submenu order and next id
SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @user_management_menu_id
), 1);

SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

-- 4) Insert submenu if not exists
INSERT INTO cfg_tbl_sub_menu (
    ser_sub_menu_id,
    ser_menu_id,
    txt_sub_menu_name,
    txt_sub_menu_url,
    int_sub_menu_order,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_view,
    bl_is_add,
    bl_is_delete,
    bl_is_update,
    bl_is_approve,
    dte_created_date,
    ser_created_user
)
SELECT
    @next_submenu_id,
    @user_management_menu_id,
    'Role Management',
    'roles',
    @next_order,
    1, 1, 0,
    1, 1, 0, 1, 0,
    NOW(),
    1
WHERE @user_management_menu_id IS NOT NULL
  AND @role_mgmt_submenu_id IS NULL;

-- 5) Resolve submenu id after insert
SET @role_mgmt_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @user_management_menu_id
      AND txt_sub_menu_name IN ('Role Management', 'Role Managment')
    LIMIT 1
);

-- 6) Normalize existing row
UPDATE cfg_tbl_sub_menu
SET
    txt_sub_menu_name = 'Role Management',
    txt_sub_menu_url = 'roles',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @role_mgmt_submenu_id
  AND @role_mgmt_submenu_id IS NOT NULL;

-- 7) Find admin role ids
-- (supports ADMIN / ROLE_ADMIN / SUPER ADMIN names)
CREATE TEMPORARY TABLE IF NOT EXISTS tmp_admin_roles (
    ser_role_id INT PRIMARY KEY
);

DELETE FROM tmp_admin_roles;

INSERT INTO tmp_admin_roles (ser_role_id)
SELECT r.ser_role_id
FROM cfg_tbl_role r
WHERE (UPPER(r.txt_role_name) LIKE '%ADMIN%')
  AND (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL);

-- 8) Grant role-level submenu permission to admin roles only
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_view,
    bl_is_add,
    bl_is_delete,
    bl_is_update,
    bl_is_approve,
    bl_is_enabled,
    bl_is_all,
    bl_is_create,
    bl_is_NewView,
    bl_is_NewUpdate,
    dte_created_date,
    ser_created_user
)
SELECT
    @role_mgmt_submenu_id,
    ar.ser_role_id,
    1, 1, 0,
    1, 1, 0, 1, 0,
    1, 0,
    1, 1, 1,
    NOW(),
    1
FROM tmp_admin_roles ar
WHERE @role_mgmt_submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @role_mgmt_submenu_id
        AND smr.ser_role_id = ar.ser_role_id
        AND smr.ser_user_id IS NULL
  );

-- 9) Keep admin role-level rows active/enabled
UPDATE cfg_tbl_sub_menu_role smr
JOIN tmp_admin_roles ar ON ar.ser_role_id = smr.ser_role_id
SET
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.bl_is_view = 1,
    smr.bl_is_add = 1,
    smr.bl_is_delete = 0,
    smr.bl_is_update = 1,
    smr.bl_is_approve = 0,
    smr.bl_is_enabled = 1,
    smr.bl_is_all = 0,
    smr.bl_is_create = 1,
    smr.bl_is_NewView = 1,
    smr.bl_is_NewUpdate = 1,
    smr.dte_modified_date = NOW(),
    smr.ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @role_mgmt_submenu_id
  AND smr.ser_user_id IS NULL;

-- 10) Disable non-admin role-level rows for this submenu (if any)
UPDATE cfg_tbl_sub_menu_role smr
LEFT JOIN tmp_admin_roles ar ON ar.ser_role_id = smr.ser_role_id
SET
    smr.bl_is_enabled = 0,
    smr.bl_is_active = 0,
    smr.bln_status = 0,
    smr.dte_modified_date = NOW(),
    smr.ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @role_mgmt_submenu_id
  AND smr.ser_user_id IS NULL
  AND ar.ser_role_id IS NULL;

-- 11) Verification
SELECT
    @user_management_menu_id AS user_management_menu_id,
    @role_mgmt_submenu_id AS role_management_submenu_id;

SELECT
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    m.txt_menu_name
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE sm.ser_sub_menu_id = @role_mgmt_submenu_id;

SELECT
    r.txt_role_name,
    smr.ser_user_id,
    smr.bl_is_enabled,
    smr.bl_is_view,
    smr.bl_is_add,
    smr.bl_is_update
FROM cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
WHERE smr.ser_sub_menu_id = @role_mgmt_submenu_id
ORDER BY r.txt_role_name, smr.ser_user_id;

DROP TEMPORARY TABLE IF EXISTS tmp_admin_roles;

