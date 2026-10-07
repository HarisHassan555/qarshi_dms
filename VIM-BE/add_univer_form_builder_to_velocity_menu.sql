-- =====================================================
-- SQL Script: Add Univer Form Builder Submenu to Velocity Menu
-- Database: vim_3
-- Description: Adds "Univer Form Builder" under Velocity/VIM and
--              grants role/user permissions.
-- =====================================================

USE vim_3;

SET @velocity_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE (txt_menu_name = 'Velocity'
           OR txt_menu_name LIKE '%Velocity%'
           OR txt_menu_name LIKE '%VIM%'
           OR txt_menu_name LIKE '%vim%')
      AND bl_is_deleted = 0
      AND bln_status = 1
    ORDER BY ser_menu_id
    LIMIT 1
);

SET @submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE (txt_sub_menu_name = 'Univer Form Builder'
           OR txt_sub_menu_url = 'univer-form-builder')
      AND (@velocity_menu_id IS NULL OR ser_menu_id = @velocity_menu_id)
    LIMIT 1
);

SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
), 1);

SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

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
    @velocity_menu_id,
    'Univer Form Builder',
    'univer-form-builder',
    @next_order,
    1,
    1,
    0,
    1,
    1,
    0,
    1,
    0,
    NOW(),
    1
WHERE @submenu_id IS NULL
  AND @velocity_menu_id IS NOT NULL;

UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @velocity_menu_id,
    txt_sub_menu_name = 'Univer Form Builder',
    txt_sub_menu_url = 'univer-form-builder',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @submenu_id
  AND @velocity_menu_id IS NOT NULL
  AND @submenu_id IS NOT NULL;

SET @submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Univer Form Builder'
      AND ser_menu_id = @velocity_menu_id
    LIMIT 1
);

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
    bl_is_new_create,
    bl_is_new_view,
    bl_is_new_update,
    dte_created_date,
    ser_created_user
)
SELECT
    @submenu_id,
    r.ser_role_id,
    CASE WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    CASE WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    0,
    1,
    1,
    0,
    1,
    0,
    CASE WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    0,
    1,
    1,
    1,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND @submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @submenu_id
        AND smr.ser_role_id = r.ser_role_id
        AND smr.ser_user_id IS NULL
  );

INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
    ser_user_id,
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
    bl_is_new_create,
    bl_is_new_view,
    bl_is_new_update,
    dte_created_date,
    ser_created_user
)
SELECT
    @submenu_id,
    u.ser_role_id,
    u.ser_user_id,
    1,
    1,
    0,
    1,
    1,
    0,
    1,
    0,
    1,
    0,
    1,
    1,
    1,
    NOW(),
    1
FROM cfg_tbl_user u
JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
WHERE u.bl_is_deleted = 0
  AND u.bln_status = 1
  AND UPPER(r.txt_role_name) LIKE '%ADMIN%'
  AND @submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @submenu_id
        AND smr.ser_user_id = u.ser_user_id
        AND smr.ser_role_id = u.ser_role_id
  );

SELECT
    sm.ser_sub_menu_id AS 'Submenu ID',
    sm.txt_sub_menu_name AS 'Submenu Name',
    sm.txt_sub_menu_url AS 'URL',
    m.txt_menu_name AS 'Menu Name',
    sm.bl_is_active AS 'Active',
    sm.bln_status AS 'Status'
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE sm.txt_sub_menu_name = 'Univer Form Builder';
