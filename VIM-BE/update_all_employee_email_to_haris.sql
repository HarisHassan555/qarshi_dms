USE vim_3;

-- =====================================================
-- Script to update all employee emails
-- New email: harishassan551@gmail.com
-- =====================================================

UPDATE hr_tbl_employee
SET
    txt_email = 'harishassan551@gmail.com',
    dte_modified_date = NOW(),
    ser_modified_user = 1  -- Change this if another user ID should be tracked
WHERE ser_employee_id >= 0
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL);

SET @updated_rows = ROW_COUNT();

SELECT
    CASE
        WHEN @updated_rows > 0 THEN CONCAT('SUCCESS - Updated ', @updated_rows, ' employee(s)')
        ELSE 'WARNING - No employee rows updated'
    END AS 'Update Status';

-- Verification
SELECT
    ser_employee_id AS 'Employee ID',
    txt_employee_code AS 'Employee Code',
    txt_employee_name AS 'Employee Name',
    txt_email AS 'Email',
    dte_modified_date AS 'Last Modified'
FROM hr_tbl_employee
WHERE (bl_is_deleted = false OR bl_is_deleted IS NULL)
ORDER BY ser_employee_id
LIMIT 200;
