-- =====================================================
-- Remove Velocity "Reports" and legacy report submenus
-- (Reports, Application Volume, Approval SLA, Department Activity)
-- Soft-delete: cfg_tbl_sub_menu_role then cfg_tbl_sub_menu
-- Database: velocity
-- =====================================================

USE velocity;

-- Preview rows that will be retired
SELECT sm.ser_sub_menu_id,
       sm.txt_sub_menu_name,
       sm.txt_sub_menu_url,
       m.txt_menu_name
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE m.txt_menu_name = 'Velocity'
  AND (
      LOWER(TRIM(sm.txt_sub_menu_url)) IN (
          'reports',
          'reports-application-volume',
          'reports-approval-sla',
          'reports-department-activity'
      )
      OR sm.txt_sub_menu_name IN (
          'Reports',
          'Application Volume Report',
          'Approval SLA Report',
          'Department Activity Report'
      )
  )
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL);

-- Role permissions for those submenus
UPDATE cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_sub_menu sm ON sm.ser_sub_menu_id = smr.ser_sub_menu_id
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
SET
    smr.bl_is_deleted = 1,
    smr.bl_is_active = 0,
    smr.bln_status = 0,
    smr.bl_is_enabled = 0,
    smr.bl_is_view = 0,
    smr.bl_is_add = 0,
    smr.bl_is_update = 0,
    smr.bl_is_delete = 0,
    smr.bl_is_approve = 0,
    smr.dte_modified_date = NOW()
WHERE m.txt_menu_name = 'Velocity'
  AND (
      LOWER(TRIM(sm.txt_sub_menu_url)) IN (
          'reports',
          'reports-application-volume',
          'reports-approval-sla',
          'reports-department-activity'
      )
      OR sm.txt_sub_menu_name IN (
          'Reports',
          'Application Volume Report',
          'Approval SLA Report',
          'Department Activity Report'
      )
  );

UPDATE cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
SET
    sm.bl_is_deleted = 1,
    sm.bl_is_active = 0,
    sm.bln_status = 0,
    sm.bl_is_view = 0,
    sm.bl_is_add = 0,
    sm.bl_is_update = 0,
    sm.bl_is_delete = 0,
    sm.bl_is_approve = 0,
    sm.dte_modified_date = NOW()
WHERE m.txt_menu_name = 'Velocity'
  AND (
      LOWER(TRIM(sm.txt_sub_menu_url)) IN (
          'reports',
          'reports-application-volume',
          'reports-approval-sla',
          'reports-department-activity'
      )
      OR sm.txt_sub_menu_name IN (
          'Reports',
          'Application Volume Report',
          'Approval SLA Report',
          'Department Activity Report'
      )
  );

-- Verify (should return no active rows)
SELECT sm.ser_sub_menu_id,
       sm.txt_sub_menu_name,
       sm.txt_sub_menu_url,
       sm.bl_is_deleted
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE m.txt_menu_name = 'Velocity'
  AND LOWER(TRIM(sm.txt_sub_menu_url)) LIKE 'reports%'
ORDER BY sm.txt_sub_menu_url;
