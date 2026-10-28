-- Reorder Master Data submenus:
--   1 Organizations, 2 Sub Areas, 3 Divisions, 4 Department, 5 Sap Employees, 6 Signature, 7 Workflow Pipelines
USE velocity;

-- Preview current order
SELECT sm.ser_sub_menu_id,
       sm.txt_sub_menu_name,
       sm.txt_sub_menu_url,
       sm.int_sub_menu_order
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE m.txt_menu_name = 'Master Data'
  AND (m.bl_is_deleted = 0 OR m.bl_is_deleted IS NULL)
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
ORDER BY sm.int_sub_menu_order, sm.txt_sub_menu_name;

UPDATE cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
SET sm.int_sub_menu_order = CASE
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) = 'organizations' THEN 1
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) IN ('sub-areas', 'subareas') THEN 2
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) = 'divisions' THEN 3
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) = 'department' THEN 4
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) = 'sap-hcm-employees' THEN 5
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) = 'signature' THEN 6
        WHEN LOWER(TRIM(sm.txt_sub_menu_url)) = 'workflow-pipelines' THEN 7
        ELSE sm.int_sub_menu_order
    END,
    sm.dte_modified_date = NOW()
WHERE m.txt_menu_name = 'Master Data'
  AND (m.bl_is_deleted = 0 OR m.bl_is_deleted IS NULL)
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND LOWER(TRIM(sm.txt_sub_menu_url)) IN (
        'organizations',
        'sub-areas',
        'subareas',
        'divisions',
        'department',
        'sap-hcm-employees',
        'signature',
        'workflow-pipelines'
    );

-- Verify after update
SELECT sm.ser_sub_menu_id,
       sm.txt_sub_menu_name,
       sm.txt_sub_menu_url,
       sm.int_sub_menu_order
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE m.txt_menu_name = 'Master Data'
  AND (m.bl_is_deleted = 0 OR m.bl_is_deleted IS NULL)
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
ORDER BY sm.int_sub_menu_order, sm.txt_sub_menu_name;
