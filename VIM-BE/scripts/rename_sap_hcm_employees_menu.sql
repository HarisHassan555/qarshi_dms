-- Rename Master Data submenu label: SAP HCM Employees -> Sap Employees
-- Route URL stays: sap-hcm-employees
USE velocity;

UPDATE cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
SET sm.txt_sub_menu_name = 'Sap Employees',
    sm.dte_modified_date = NOW()
WHERE m.txt_menu_name = 'Master Data'
  AND (m.bl_is_deleted = 0 OR m.bl_is_deleted IS NULL)
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (
        LOWER(TRIM(sm.txt_sub_menu_url)) = 'sap-hcm-employees'
        OR sm.txt_sub_menu_name IN ('SAP HCM Employees', 'SAP HCM Employee')
    );

SELECT sm.ser_sub_menu_id, sm.txt_sub_menu_name, sm.txt_sub_menu_url, sm.int_sub_menu_order
FROM cfg_tbl_sub_menu sm
INNER JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE LOWER(TRIM(sm.txt_sub_menu_url)) = 'sap-hcm-employees';
