-- Enable Workflow Pipelines submenu for all roles (view + enabled).
-- Run after add_workflow_pipelines_master_data_menu.sql if users were seeded with bl_is_enabled = 0.

USE velocity;

UPDATE cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON sm.ser_sub_menu_id = smr.ser_sub_menu_id
SET
    smr.bl_is_enabled = 1,
    smr.bl_is_view = 1,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.dte_modified_date = NOW()
WHERE LOWER(TRIM(sm.txt_sub_menu_url)) = 'workflow-pipelines'
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0);

SELECT
    r.txt_role_name,
    smr.bl_is_enabled,
    smr.bl_is_view
FROM cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_sub_menu sm ON sm.ser_sub_menu_id = smr.ser_sub_menu_id
JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
WHERE LOWER(TRIM(sm.txt_sub_menu_url)) = 'workflow-pipelines'
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
ORDER BY r.txt_role_name;
