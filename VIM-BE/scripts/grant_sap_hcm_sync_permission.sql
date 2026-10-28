-- Grant SAP HCM sync permission (Update/Add) on submenu sap-hcm-employees
-- Required by SapHcmEmployeeController.hasSyncPermission() — View alone is not enough.
-- Run on staging/production DB (default schema: velocity).

USE velocity;

SET @sap_hcm_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE LOWER(TRIM(txt_sub_menu_url)) = 'sap-hcm-employees'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT @sap_hcm_submenu_id AS sap_hcm_submenu_id;

-- 1) Role-level rows: enable sync for roles that already have this submenu (view or any flag)
UPDATE cfg_tbl_sub_menu_role smr
SET
    smr.bl_is_view = COALESCE(smr.bl_is_view, 1),
    smr.bl_is_update = 1,
    smr.bl_is_add = 1,
    smr.bl_is_enabled = 1,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @sap_hcm_submenu_id
  AND @sap_hcm_submenu_id IS NOT NULL
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  AND (smr.bl_is_deleted = 0 OR smr.bl_is_deleted IS NULL)
  AND (
      smr.bl_is_view = 1 OR smr.bl_is_new_view = 1
      OR UPPER(COALESCE((
          SELECT r.txt_role_name FROM cfg_tbl_role r WHERE r.ser_role_id = smr.ser_role_id LIMIT 1
      ), '')) LIKE '%ADMIN%'
  );

-- 2) ADMIN roles missing a row: insert role permission with view + sync
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
    @sap_hcm_submenu_id,
    r.ser_role_id,
    1, 1, 0,
    1, 1, 0, 1, 0,
    1, 0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE @sap_hcm_submenu_id IS NOT NULL
  AND (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL)
  AND UPPER(r.txt_role_name) LIKE '%ADMIN%'
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @sap_hcm_submenu_id
        AND smr.ser_role_id = r.ser_role_id
        AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
        AND (smr.bl_is_deleted = 0 OR smr.bl_is_deleted IS NULL)
  );

-- 3) User-level overrides: if a user has view-only row, grant sync on that row too
UPDATE cfg_tbl_sub_menu_role smr
SET
    smr.bl_is_update = 1,
    smr.bl_is_add = 1,
    smr.bl_is_enabled = 1,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @sap_hcm_submenu_id
  AND @sap_hcm_submenu_id IS NOT NULL
  AND smr.ser_user_id IS NOT NULL
  AND smr.ser_user_id > 0
  AND (smr.bl_is_deleted = 0 OR smr.bl_is_deleted IS NULL);

-- Verify (replace YOUR_USER_ID with ser_user_id you test with)
-- SELECT smr.*, sm.txt_sub_menu_url, r.txt_role_name
-- FROM cfg_tbl_sub_menu_role smr
-- JOIN cfg_tbl_sub_menu sm ON sm.ser_sub_menu_id = smr.ser_sub_menu_id
-- LEFT JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
-- WHERE LOWER(TRIM(sm.txt_sub_menu_url)) = 'sap-hcm-employees'
--   AND (smr.ser_user_id = YOUR_USER_ID OR smr.ser_user_id IS NULL OR smr.ser_user_id = 0);
