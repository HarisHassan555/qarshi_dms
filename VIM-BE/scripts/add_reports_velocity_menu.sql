-- =====================================================
-- Single "Reports" submenu under Velocity (admin-enabled by default)
-- Frontend route: reports  →  /velocity/reports
-- Advanced search UI with sample document corpus (FE only until API is wired)
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

-- Retire older multi-report submenu rows if this script was run before
UPDATE cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
SET
    sm.bl_is_deleted = 1,
    sm.bl_is_active = 0,
    sm.bln_status = 0,
    sm.dte_modified_date = NOW()
WHERE m.txt_menu_name = 'Velocity'
  AND sm.txt_sub_menu_url IN (
      'reports-application-volume',
      'reports-approval-sla',
      'reports-department-activity'
  );

SET @velocity_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Velocity'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
      AND (bln_status = 1 OR bln_status IS NULL)
    LIMIT 1
);

SET @reports_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
      AND LOWER(TRIM(txt_sub_menu_url)) = 'reports'
    LIMIT 1
);

SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
), 50);

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
    'Reports',
    'reports',
    @next_order,
    1, 1, 0,
    1, 0, 0, 0, 0,
    NOW(),
    1
WHERE @velocity_menu_id IS NOT NULL
  AND @reports_submenu_id IS NULL;

SET @reports_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
      AND LOWER(TRIM(txt_sub_menu_url)) = 'reports'
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    txt_sub_menu_name = 'Reports',
    txt_sub_menu_url = 'reports',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @reports_submenu_id
  AND @reports_submenu_id IS NOT NULL
LIMIT 1;

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
    @reports_submenu_id,
    r.ser_role_id,
    1, 1, 0,
    CASE WHEN UPPER(TRIM(r.txt_role_name)) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    0, 0, 0, 0,
    CASE WHEN UPPER(TRIM(r.txt_role_name)) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL)
  AND @reports_submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @reports_submenu_id
        AND smr.ser_role_id = r.ser_role_id
        AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  );

UPDATE cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
SET
    smr.bl_is_view = CASE WHEN UPPER(TRIM(r.txt_role_name)) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    smr.bl_is_enabled = CASE WHEN UPPER(TRIM(r.txt_role_name)) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @reports_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0);

SELECT
    @velocity_menu_id AS velocity_menu_id,
    @reports_submenu_id AS reports_submenu_id;

SELECT
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    sm.int_sub_menu_order,
    m.txt_menu_name
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE sm.ser_sub_menu_id = @reports_submenu_id;

SELECT
    r.txt_role_name,
    smr.bl_is_view,
    smr.bl_is_enabled
FROM cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
WHERE smr.ser_sub_menu_id = @reports_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
ORDER BY r.txt_role_name;
