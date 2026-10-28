-- =====================================================
-- Add "Workflow Pipelines" under Master Data (last position: order 7)
-- Angular route: workflow-pipelines
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

SET @master_data_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Master Data'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
      AND (bln_status = 1 OR bln_status IS NULL)
    LIMIT 1
);

SET @workflow_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @master_data_menu_id
      AND LOWER(TRIM(txt_sub_menu_url)) = 'workflow-pipelines'
    LIMIT 1
);

SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

-- Last in Master Data: after Signature (order 6)
SET @menu_order = 7;

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
    @master_data_menu_id,
    'Workflow Pipelines',
    'workflow-pipelines',
    @menu_order,
    1, 1, 0,
    1, 1, 0, 1, 0,
    NOW(),
    1
WHERE @master_data_menu_id IS NOT NULL
  AND @workflow_submenu_id IS NULL;

SET @workflow_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @master_data_menu_id
      AND LOWER(TRIM(txt_sub_menu_url)) = 'workflow-pipelines'
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    txt_sub_menu_name = 'Workflow Pipelines',
    txt_sub_menu_url = 'workflow-pipelines',
    int_sub_menu_order = @menu_order,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @workflow_submenu_id
  AND @workflow_submenu_id IS NOT NULL;

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
    @workflow_submenu_id,
    r.ser_role_id,
    1, 1, 0,
    1, 1, 0, 1, 0,
    1,
    0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL)
  AND @workflow_submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @workflow_submenu_id
        AND smr.ser_role_id = r.ser_role_id
        AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  );

UPDATE cfg_tbl_sub_menu_role smr
SET
    smr.bl_is_enabled = 1,
    smr.bl_is_view = 1,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @workflow_submenu_id
  AND @workflow_submenu_id IS NOT NULL
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0);

SELECT
    @master_data_menu_id AS master_data_menu_id,
    @workflow_submenu_id AS workflow_pipelines_submenu_id,
    @menu_order AS int_sub_menu_order;

SELECT
    sm.ser_sub_menu_id,
    sm.txt_sub_menu_name,
    sm.txt_sub_menu_url,
    sm.int_sub_menu_order,
    m.txt_menu_name
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE sm.ser_sub_menu_id = @workflow_submenu_id;
