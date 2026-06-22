-- =====================================================
-- Add "Activity Logs" submenu under "User Management"
-- URL: activitylogs
-- Database: velocity_workbench (change USE if needed)
-- =====================================================

USE velocity_workbench;

SET @user_management_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'User Management'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
      AND (bln_status = 1 OR bln_status IS NULL)
    LIMIT 1
);

SET @activity_logs_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @user_management_menu_id
      AND txt_sub_menu_name = 'Activity Logs'
    LIMIT 1
);

SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @user_management_menu_id
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
    @user_management_menu_id,
    'Activity Logs',
    'activitylogs',
    @next_order,
    1, 1, 0,
    1, 1, 0, 1, 0,
    NOW(),
    1
WHERE @user_management_menu_id IS NOT NULL
  AND @activity_logs_submenu_id IS NULL;

SET @activity_logs_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @user_management_menu_id
      AND txt_sub_menu_name = 'Activity Logs'
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    txt_sub_menu_name = 'Activity Logs',
    txt_sub_menu_url = 'activitylogs',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @activity_logs_submenu_id
  AND @activity_logs_submenu_id IS NOT NULL;

-- Grant to all active roles (enable/disable per role from /roles page)
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
    @activity_logs_submenu_id,
    r.ser_role_id,
    1, 1, 0,
    1, 1, 0, 1, 0,
    CASE WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL)
  AND @activity_logs_submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @activity_logs_submenu_id
        AND smr.ser_role_id = r.ser_role_id
        AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  );

SELECT
    @user_management_menu_id AS user_management_menu_id,
    @activity_logs_submenu_id AS activity_logs_submenu_id;

SELECT
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    m.txt_menu_name
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE sm.ser_sub_menu_id = @activity_logs_submenu_id;

SELECT
    r.txt_role_name,
    smr.bl_is_enabled,
    smr.bl_is_view,
    smr.bl_is_update
FROM cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
WHERE smr.ser_sub_menu_id = @activity_logs_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
ORDER BY r.txt_role_name;
