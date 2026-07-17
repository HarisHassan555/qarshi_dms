-- =====================================================
-- SQL Script: Add Department Application Submenu to Velocity Menu
-- Database: vim_3
-- Description: Adds "Department Application" submenu under Velocity/VIM
--              so it appears in Roles > Assign Menu and Submenu Permissions
-- =====================================================

USE vim_3;

SET @velocity_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE (txt_menu_name = 'Velocity'
           OR txt_menu_name LIKE '%Velocity%'
           OR txt_menu_name LIKE '%VIM%')
      AND bl_is_deleted = 0
      AND bln_status = 1
    ORDER BY ser_menu_id
    LIMIT 1
);

SET @base_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
), 1);

SET @department_application_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE (txt_sub_menu_name = 'Department Application'
           OR txt_sub_menu_url = 'department-application')
      AND (@velocity_menu_id IS NULL OR ser_menu_id = @velocity_menu_id)
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @velocity_menu_id,
    txt_sub_menu_name = 'Department Application',
    txt_sub_menu_url = 'department-application',
    int_sub_menu_order = @base_order,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @department_application_submenu_id
  AND @velocity_menu_id IS NOT NULL
  AND @department_application_submenu_id IS NOT NULL;

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
    'Department Application',
    'department-application',
    @base_order,
    1,
    1,
    0,
    1,
    0,
    0,
    0,
    0,
    NOW(),
    1
WHERE @department_application_submenu_id IS NULL
  AND @velocity_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_url = 'department-application'
  );

SET @department_application_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_url = 'department-application'
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
    dte_created_date,
    ser_created_user
)
SELECT
    @department_application_submenu_id,
    r.ser_role_id,
    1,
    1,
    0,
    1,
    0,
    0,
    0,
    0,
    CASE
        WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1
        ELSE 0
    END,
    0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE r.bl_is_deleted = 0
  AND r.bln_status = 1
  AND @department_application_submenu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @department_application_submenu_id
      AND smr.ser_role_id = r.ser_role_id
      AND smr.ser_user_id IS NULL
  );

SELECT
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    m.txt_menu_name
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE sm.txt_sub_menu_url = 'department-application';
