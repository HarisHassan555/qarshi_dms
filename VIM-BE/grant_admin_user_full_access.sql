USE vim_3;

-- =====================================================
-- Script to grant "admin" user access to ALL screens
-- and full permissions to edit in /roles page
-- =====================================================

-- Step 1: Find the "admin" user
SET @admin_user_id = (
    SELECT ser_user_id
    FROM cfg_tbl_user
    WHERE txt_user_name = 'admin'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @admin_username = (
    SELECT txt_user_name
    FROM cfg_tbl_user
    WHERE ser_user_id = @admin_user_id
    LIMIT 1
);

SET @admin_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_user
    WHERE ser_user_id = @admin_user_id
    LIMIT 1
);

SELECT 
    @admin_user_id AS 'Admin User ID',
    @admin_username AS 'Admin Username',
    @admin_role_id AS 'Admin Role ID',
    CASE
        WHEN @admin_user_id IS NULL THEN 'ERROR - admin user not found'
        ELSE 'SUCCESS - admin user found'
    END AS 'Status';

-- Step 2: Get ALL submenus
SELECT 
    COUNT(*) AS 'Total Submenus',
    GROUP_CONCAT(txt_sub_menu_name ORDER BY txt_sub_menu_name SEPARATOR ', ') AS 'Submenu Names'
FROM cfg_tbl_sub_menu
WHERE (bl_is_deleted = false OR bl_is_deleted IS NULL)
  AND (bln_status = true OR bln_status IS NULL);

-- =====================================================
-- Step 3: Grant admin user access to ALL submenus
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
    bl_is_create,
    bl_is_NewView,
    bl_is_NewUpdate,
    dte_created_date,
    ser_created_user
)
SELECT 
    sm.ser_sub_menu_id,
    @admin_role_id,
    @admin_user_id,
    1,  -- Active
    1,  -- Status active
    0,  -- Not deleted
    1,  -- View permission (full access)
    1,  -- Add permission (full access)
    1,  -- Delete permission (full access)
    1,  -- Update permission (full access)
    1,  -- Approve permission (full access)
    1,  -- Enabled
    1,  -- All permissions
    1,  -- bl_is_create (blIsNewCreate) - Required for canAdd check
    1,  -- bl_is_NewView (blIsNewView)
    1,  -- bl_is_NewUpdate (blIsNewUpdate) - Required for canUpdate check
    NOW(),
    1   -- Created by user ID 1
FROM cfg_tbl_sub_menu sm
WHERE (sm.bl_is_deleted = false OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = true OR sm.bln_status IS NULL)
  AND @admin_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = @admin_user_id
  );

SELECT CONCAT('Created permissions for ', ROW_COUNT(), ' submenus') AS 'Status';

-- =====================================================
-- Step 4: Update existing permissions to ensure full access
-- =====================================================
UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 1,
    bl_is_update = 1,
    bl_is_approve = 1,
    bl_is_enabled = 1,
    bl_is_all = 1,
    bl_is_create = 1,  -- blIsNewCreate - Required for canAdd check
    bl_is_NewView = 1,  -- blIsNewView
    bl_is_NewUpdate = 1,  -- blIsNewUpdate - Required for canUpdate check
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_user_id = @admin_user_id
  AND smr.ser_role_id = @admin_role_id
  AND @admin_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL;

SELECT CONCAT('Updated permissions for ', ROW_COUNT(), ' existing submenu roles') AS 'Status';

-- =====================================================
-- Step 5: Ensure admin has access to "Permission" submenu
-- =====================================================
SET @permission_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_name = 'Permission'
      AND (bl_is_deleted = false OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT 
    @permission_submenu_id AS 'Permission Submenu ID',
    CASE
        WHEN @permission_submenu_id IS NULL THEN 'WARNING - Permission submenu not found'
        ELSE 'SUCCESS - Permission submenu found'
    END AS 'Status';

-- Ensure Permission submenu has full access for admin (including new permission fields)
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
    bl_is_create,
    bl_is_NewView,
    bl_is_NewUpdate,
    dte_created_date,
    ser_created_user
)
SELECT 
    @permission_submenu_id,
    @admin_role_id,
    @admin_user_id,
    1, 1, 0, 1, 1, 1, 1, 1, 1, 1,
    1,  -- bl_is_create (blIsNewCreate) - CRITICAL for canAdd('Permission')
    1,  -- bl_is_NewView (blIsNewView)
    1,  -- bl_is_NewUpdate (blIsNewUpdate) - CRITICAL for canUpdate('permission')
    NOW(),
    1
WHERE @permission_submenu_id IS NOT NULL
  AND @admin_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = @permission_submenu_id
      AND smr.ser_role_id = @admin_role_id
      AND smr.ser_user_id = @admin_user_id
  );

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 1,
    bl_is_update = 1,
    bl_is_approve = 1,
    bl_is_enabled = 1,
    bl_is_all = 1,
    bl_is_create = 1,  -- blIsNewCreate - CRITICAL for canAdd('Permission')
    bl_is_NewView = 1,  -- blIsNewView
    bl_is_NewUpdate = 1,  -- blIsNewUpdate - CRITICAL for canUpdate('permission')
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_sub_menu_id = @permission_submenu_id
  AND smr.ser_user_id = @admin_user_id
  AND smr.ser_role_id = @admin_role_id
  AND @permission_submenu_id IS NOT NULL
  AND @admin_user_id IS NOT NULL
  AND @admin_role_id IS NOT NULL;

-- =====================================================
-- Step 6: Verification
-- =====================================================
SELECT 'Verification: Admin User Permissions Summary' AS 'Check';
SELECT 
    COUNT(*) AS 'Total Submenu Permissions',
    COUNT(DISTINCT smr.ser_sub_menu_id) AS 'Unique Submenus',
    SUM(CASE WHEN smr.bl_is_enabled = 1 THEN 1 ELSE 0 END) AS 'Enabled Permissions',
    SUM(CASE WHEN smr.bl_is_view = 1 THEN 1 ELSE 0 END) AS 'Can View',
    SUM(CASE WHEN smr.bl_is_add = 1 THEN 1 ELSE 0 END) AS 'Can Add',
    SUM(CASE WHEN smr.bl_is_update = 1 THEN 1 ELSE 0 END) AS 'Can Update',
    SUM(CASE WHEN smr.bl_is_delete = 1 THEN 1 ELSE 0 END) AS 'Can Delete',
    SUM(CASE WHEN smr.bl_is_approve = 1 THEN 1 ELSE 0 END) AS 'Can Approve',
    SUM(CASE WHEN smr.bl_is_all = 1 THEN 1 ELSE 0 END) AS 'All Permissions'
FROM cfg_tbl_sub_menu_role smr
WHERE smr.ser_user_id = @admin_user_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.bl_is_deleted = 0;

-- Check Permission submenu specifically (including new permission fields)
SELECT 
    'Permission Submenu Access' AS 'Check',
    sm.txt_sub_menu_name AS 'Submenu',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'Can View',
    smr.bl_is_add AS 'Can Add',
    smr.bl_is_update AS 'Can Update',
    smr.bl_is_delete AS 'Can Delete',
    smr.bl_is_approve AS 'Can Approve',
    smr.bl_is_all AS 'All Permissions',
    smr.bl_is_create AS 'blIsNewCreate (canAdd)',
    smr.bl_is_NewView AS 'blIsNewView',
    smr.bl_is_NewUpdate AS 'blIsNewUpdate (canUpdate)'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
WHERE sm.txt_sub_menu_name = 'Permission'
  AND smr.ser_user_id = @admin_user_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.bl_is_deleted = 0;

-- List all submenus admin has access to
SELECT 
    'All Submenus Admin Has Access To' AS 'Check',
    sm.txt_sub_menu_name AS 'Submenu Name',
    m.txt_menu_name AS 'Parent Menu',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.bl_is_delete AS 'Delete',
    smr.bl_is_approve AS 'Approve'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
LEFT JOIN cfg_tbl_menu m ON sm.ser_menu_id = m.ser_menu_id
WHERE smr.ser_user_id = @admin_user_id
  AND smr.ser_role_id = @admin_role_id
  AND smr.bl_is_deleted = 0
ORDER BY m.txt_menu_name, sm.txt_sub_menu_name;

-- =====================================================
-- Step 7: Final Status
-- =====================================================
SELECT 
    'FINAL STATUS' AS 'Check',
    CASE 
        WHEN @admin_user_id IS NOT NULL
         AND EXISTS (
             SELECT 1 FROM cfg_tbl_sub_menu_role smr
             WHERE smr.ser_user_id = @admin_user_id
               AND smr.ser_role_id = @admin_role_id
               AND smr.bl_is_enabled = 1
         )
         AND EXISTS (
             SELECT 1 FROM cfg_tbl_sub_menu_role smr
             INNER JOIN cfg_tbl_sub_menu sm ON smr.ser_sub_menu_id = sm.ser_sub_menu_id
             WHERE sm.txt_sub_menu_name = 'Permission'
               AND smr.ser_user_id = @admin_user_id
               AND smr.ser_role_id = @admin_role_id
               AND smr.bl_is_enabled = 1
               AND smr.bl_is_view = 1
               AND smr.bl_is_update = 1
               AND smr.bl_is_create = 1  -- Required for canAdd('Permission')
               AND smr.bl_is_NewUpdate = 1  -- Required for canUpdate('permission')
         )
        THEN CONCAT('SUCCESS - Admin user (', IFNULL(@admin_username, 'N/A'), ') has full access to all screens and can edit in /roles page - RESTART BACKEND and LOGOUT/LOGIN')
        WHEN @admin_user_id IS NULL
        THEN 'ERROR - admin user not found. Please check if user "admin" exists in cfg_tbl_user table.'
        ELSE 'WARNING - Some permissions may be missing. Check verification queries above.'
    END AS 'Status';
