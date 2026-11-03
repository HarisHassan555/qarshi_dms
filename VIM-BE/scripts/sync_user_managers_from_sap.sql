-- Set cfg_tbl_user.manager_id from SAP HCM txt_report_to (manager employee code).
-- Match: user.txt_cnic = employee.txt_employee_id, manager.txt_cnic = employee.txt_report_to
-- Database: velocity (change USE if needed)

USE velocity;

SET @OLD_SQL_SAFE_UPDATES = @@SESSION.SQL_SAFE_UPDATES;
SET SESSION SQL_SAFE_UPDATES = 0;

UPDATE cfg_tbl_user u
INNER JOIN cfg_tbl_sap_hcm_employee e
    ON TRIM(e.txt_employee_id) = TRIM(u.txt_cnic)
   AND (e.bl_is_deleted = 0 OR e.bl_is_deleted IS NULL)
INNER JOIN cfg_tbl_user m
    ON TRIM(m.txt_cnic) = TRIM(e.txt_report_to)
   AND (m.bl_is_deleted = 0 OR m.bl_is_deleted IS NULL)
SET
    u.manager_id = m.ser_user_id,
    u.dte_modified_date = NOW()
WHERE (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
  AND u.ser_user_id > 0
  AND TRIM(IFNULL(e.txt_report_to, '')) <> ''
  AND UPPER(TRIM(e.txt_report_to)) NOT IN ('0', '00000000')
  AND u.ser_user_id <> m.ser_user_id;

SET SESSION SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

SELECT
    COUNT(*) AS users_with_manager
FROM cfg_tbl_user u
WHERE (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
  AND u.manager_id IS NOT NULL;

SELECT
    u.ser_user_id,
    u.txt_user_name,
    u.txt_cnic AS employee_id,
    m.ser_user_id AS manager_user_id,
    m.txt_user_name AS manager_name,
    m.txt_cnic AS manager_employee_id
FROM cfg_tbl_user u
LEFT JOIN cfg_tbl_user m ON m.ser_user_id = u.manager_id
WHERE (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
ORDER BY u.txt_user_name
LIMIT 50;
