USE vim_3;

-- =====================================================
-- Script to grant all Admin role users access to new screens
-- New screens: Department, CAPF, Application, Form Builder, Applications View
-- =====================================================

-- Step 1: Find Admin role ID
SET @admin_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_role
    WHERE (txt_role_name LIKE '%ADMIN%' OR txt_role_name LIKE '%admin%' OR txt_role_name LIKE '%Admin%')
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
      AND (bl_is_active = true OR bl_is_active IS NULL)
    ORDER BY ser_role_id
    LIMIT 1
);

SELECT 
    @admin_role_id AS 'Admin Role ID',
    CASE
        WHEN @admin_role_id IS NULL THEN 'ERROR - Admin role not found'
        ELSE 'SUCCESS - Admin role found'
    END AS 'Status';

-- Step 2: Get all Admin role users
SELECT 
    COUNT(*) AS 'Total Admin Users',
    GROUP_CONCAT(txt_user_name SEPARATOR ', ') AS 'Admin Users'
FROM cfg_tbl_user
WHERE ser_role_id = @admin_role_id
  AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
  AND (bln_status = true OR bln_status IS NULL)
  AND @admin_role_id IS NOT NULL;

-- Step 3: Get submenu IDs for new screens
SET @dept_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Department'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @capf_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'CAPF'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @application_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Application'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @form_builder_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Form Builder'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @applications_view_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Applications View'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

-- Step 4: Verify submenu IDs
SELECT 
    'Department' AS 'Submenu',
    @dept_submenu_id AS 'Submenu ID',
    CASE WHEN @dept_submenu_id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END AS 'Status'
UNION ALL
SELECT 
    'CAPF',
    @capf_submenu_id,
    CASE WHEN @capf_submenu_id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END
UNION ALL
SELECT 
    'Application',
    @application_submenu_id,
    CASE WHEN @application_submenu_id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END
UNION ALL
SELECT 
    'Form Builder',
    @form_builder_submenu_id,
    CASE WHEN @form_builder_submenu_id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END
UNION ALL
SELECT 
    'Applications View',
    @applications_view_submenu_id,
    CASE WHEN @applications_view_submenu_id IS NULL THEN 'NOT FOUND' ELSE 'FOUND' END;

-- =====================================================
-- Step 5: Grant permissions for Department submenu
-- =====================================================
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
    ser_user_id,
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
    @dept_submenu_id,
    @admin_role_id,
    u.ser_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission
    1,  -- Add permission
    0,  -- Delete permission
    1,  -- Update permission
    0,  -- Approve permission
    1,  -- Enabled
    0,  -- Not all permissions
    NOW(),
    1   -- Created by user ID 1
FROM cfg_tbl_user u
WHERE u.ser_role_id = @admin_role_id
  AND u.bl_is_deleted = 0
  AND (u.bln_status = 1 OR u.bln_status IS NULL)
  AND @dept_submenu_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @dept_submenu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = u.ser_user_id
  );

SELECT CONCAT('Department: ', ROW_COUNT(), ' Admin user permissions created/updated') AS 'Status';

-- Update existing permissions to ensure they are enabled
UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @dept_submenu_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.ser_user_id IN (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE ser_role_id = @admin_role_id
      AND bl_is_deleted = 0
      AND (bln_status = 1 OR bln_status IS NULL)
  )
  AND @dept_submenu_id IS NOT NULL
  AND @admin_role_id IS NOT NULL;

-- =====================================================
-- Step 6: Grant permissions for CAPF submenu
-- =====================================================
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
    ser_user_id,
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
    @capf_submenu_id,
    @admin_role_id,
    u.ser_user_id,
    1, 1, 0, 1, 1, 0, 1, 0, 1, 0,
    NOW(),
    1
FROM cfg_tbl_user u
WHERE u.ser_role_id = @admin_role_id
  AND u.bl_is_deleted = 0
  AND (u.bln_status = 1 OR u.bln_status IS NULL)
  AND @capf_submenu_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @capf_submenu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = u.ser_user_id
  );

SELECT CONCAT('CAPF: ', ROW_COUNT(), ' Admin user permissions created/updated') AS 'Status';

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1, bln_status = 1, bl_is_deleted = 0,
    bl_is_view = 1, bl_is_add = 1, bl_is_delete = 0, bl_is_update = 1, bl_is_approve = 0,
    bl_is_enabled = 1, bl_is_all = 0,
    dte_modified_date = NOW(), ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @capf_submenu_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.ser_user_id IN (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE ser_role_id = @admin_role_id AND bl_is_deleted = 0 AND (bln_status = 1 OR bln_status IS NULL)
  )
  AND @capf_submenu_id IS NOT NULL AND @admin_role_id IS NOT NULL;

-- =====================================================
-- Step 7: Grant permissions for Application submenu
-- =====================================================
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id, ser_role_id, ser_user_id,
    bl_is_active, bln_status, bl_is_deleted,
    bl_is_view, bl_is_add, bl_is_delete, bl_is_update, bl_is_approve,
    bl_is_enabled, bl_is_all,
    dte_created_date, ser_created_user
)
SELECT 
    @application_submenu_id, @admin_role_id, u.ser_user_id,
    1, 1, 0, 1, 1, 0, 1, 0, 1, 0,
    NOW(), 1
FROM cfg_tbl_user u
WHERE u.ser_role_id = @admin_role_id
  AND u.bl_is_deleted = 0
  AND (u.bln_status = 1 OR u.bln_status IS NULL)
  AND @application_submenu_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @application_submenu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = u.ser_user_id
  );

SELECT CONCAT('Application: ', ROW_COUNT(), ' Admin user permissions created/updated') AS 'Status';

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1, bln_status = 1, bl_is_deleted = 0,
    bl_is_view = 1, bl_is_add = 1, bl_is_delete = 0, bl_is_update = 1, bl_is_approve = 0,
    bl_is_enabled = 1, bl_is_all = 0,
    dte_modified_date = NOW(), ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @application_submenu_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.ser_user_id IN (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE ser_role_id = @admin_role_id AND bl_is_deleted = 0 AND (bln_status = 1 OR bln_status IS NULL)
  )
  AND @application_submenu_id IS NOT NULL AND @admin_role_id IS NOT NULL;

-- =====================================================
-- Step 8: Grant permissions for Form Builder submenu
-- =====================================================
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id, ser_role_id, ser_user_id,
    bl_is_active, bln_status, bl_is_deleted,
    bl_is_view, bl_is_add, bl_is_delete, bl_is_update, bl_is_approve,
    bl_is_enabled, bl_is_all,
    dte_created_date, ser_created_user
)
SELECT 
    @form_builder_submenu_id, @admin_role_id, u.ser_user_id,
    1, 1, 0, 1, 1, 0, 1, 0, 1, 0,
    NOW(), 1
FROM cfg_tbl_user u
WHERE u.ser_role_id = @admin_role_id
  AND u.bl_is_deleted = 0
  AND (u.bln_status = 1 OR u.bln_status IS NULL)
  AND @form_builder_submenu_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @form_builder_submenu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = u.ser_user_id
  );

SELECT CONCAT('Form Builder: ', ROW_COUNT(), ' Admin user permissions created/updated') AS 'Status';

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1, bln_status = 1, bl_is_deleted = 0,
    bl_is_view = 1, bl_is_add = 1, bl_is_delete = 0, bl_is_update = 1, bl_is_approve = 0,
    bl_is_enabled = 1, bl_is_all = 0,
    dte_modified_date = NOW(), ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @form_builder_submenu_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.ser_user_id IN (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE ser_role_id = @admin_role_id AND bl_is_deleted = 0 AND (bln_status = 1 OR bln_status IS NULL)
  )
  AND @form_builder_submenu_id IS NOT NULL AND @admin_role_id IS NOT NULL;

-- =====================================================
-- Step 9: Grant permissions for Applications View submenu
-- =====================================================
INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id, ser_role_id, ser_user_id,
    bl_is_active, bln_status, bl_is_deleted,
    bl_is_view, bl_is_add, bl_is_delete, bl_is_update, bl_is_approve,
    bl_is_enabled, bl_is_all,
    dte_created_date, ser_created_user
)
SELECT 
    @applications_view_submenu_id, @admin_role_id, u.ser_user_id,
    1, 1, 0, 1, 1, 1, 1, 0, 1, 0,
    NOW(), 1
FROM cfg_tbl_user u
WHERE u.ser_role_id = @admin_role_id
  AND u.bl_is_deleted = 0
  AND (u.bln_status = 1 OR u.bln_status IS NULL)
  AND @applications_view_submenu_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @applications_view_submenu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = u.ser_user_id
  );

SELECT CONCAT('Applications View: ', ROW_COUNT(), ' Admin user permissions created/updated') AS 'Status';

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1, bln_status = 1, bl_is_deleted = 0,
    bl_is_view = 1, bl_is_add = 1, bl_is_delete = 1, bl_is_update = 1, bl_is_approve = 0,
    bl_is_enabled = 1, bl_is_all = 0,
    dte_modified_date = NOW(), ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @applications_view_submenu_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.ser_user_id IN (
    SELECT ser_user_id FROM cfg_tbl_user
    WHERE ser_role_id = @admin_role_id AND bl_is_deleted = 0 AND (bln_status = 1 OR bln_status IS NULL)
  )
  AND @applications_view_submenu_id IS NOT NULL AND @admin_role_id IS NOT NULL;

-- =====================================================
-- Step 10: Verification
-- =====================================================
SELECT 'Verification: Admin User Permissions Summary' AS 'Check';
SELECT 
    sm.txt_sub_menu_name AS 'Submenu',
    COUNT(DISTINCT smr.ser_user_id) AS 'Admin Users with Access',
    COUNT(*) AS 'Total Permissions',
    SUM(CASE WHEN smr.bl_is_enabled = 1 THEN 1 ELSE 0 END) AS 'Enabled Permissions'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
INNER JOIN cfg_tbl_user u ON smr.ser_user_id = u.ser_user_id
WHERE sm.txt_sub_menu_name IN ('Department', 'CAPF', 'Application', 'Form Builder', 'Applications View')
  AND smr.ser_role_id = @admin_role_id
  AND u.ser_role_id = @admin_role_id
  AND smr.bl_is_deleted = 0
GROUP BY sm.txt_sub_menu_name
ORDER BY sm.txt_sub_menu_name;

SELECT 
    'FINAL STATUS' AS 'Check',
    CASE 
        WHEN @admin_role_id IS NOT NULL
         AND @dept_submenu_id IS NOT NULL
         AND @capf_submenu_id IS NOT NULL
         AND @application_submenu_id IS NOT NULL
         AND @form_builder_submenu_id IS NOT NULL
         AND @applications_view_submenu_id IS NOT NULL
         AND EXISTS (
             SELECT 1 FROM cfg_tbl_sub_menu_role smr
             WHERE smr.ser_role_id = @admin_role_id
               AND smr.ser_sub_menu_id IN (@dept_submenu_id, @capf_submenu_id, @application_submenu_id, @form_builder_submenu_id, @applications_view_submenu_id)
               AND smr.bl_is_enabled = 1
         )
        THEN '✅ All Admin users have access to new screens - RESTART BACKEND and LOGOUT/LOGIN'
        ELSE '❌ Some permissions may be missing - check verification queries above'
    END AS 'Status';






