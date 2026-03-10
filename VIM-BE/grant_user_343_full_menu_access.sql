USE vim_3;

-- =====================================================
-- Script: Grant full menu/submenu access to specific user
-- Target User: ser_user_id = 343
-- =====================================================

SET @target_user_id = 343;

-- Step 1: Validate user and fetch role
SET @target_user_name = (
    SELECT txt_user_name
    FROM cfg_tbl_user
    WHERE ser_user_id = @target_user_id
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @target_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_user
    WHERE ser_user_id = @target_user_id
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SELECT
    @target_user_id AS 'User ID',
    @target_user_name AS 'Username',
    @target_role_id AS 'Role ID',
    CASE
        WHEN @target_user_name IS NULL THEN 'ERROR - user not found or deleted'
        WHEN @target_role_id IS NULL THEN 'ERROR - user role not found'
        ELSE 'SUCCESS - user found'
    END AS 'Status';

-- Step 2: Insert missing permissions for all active submenus
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
    @target_role_id,
    @target_user_id,
    1,
    1,
    0,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    1,
    NOW(),
    1
FROM cfg_tbl_sub_menu sm
WHERE (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
  AND @target_user_name IS NOT NULL
  AND @target_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
        AND smr.ser_user_id = @target_user_id
  );

SELECT CONCAT('Inserted missing submenu permissions: ', ROW_COUNT()) AS 'Insert Status';

-- Step 3: Update all existing permissions for this user to full access
UPDATE cfg_tbl_sub_menu_role smr
SET
    ser_role_id = @target_role_id,
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
    bl_is_create = 1,
    bl_is_NewView = 1,
    bl_is_NewUpdate = 1,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_user_id = @target_user_id
  AND @target_user_name IS NOT NULL
  AND @target_role_id IS NOT NULL;

SELECT CONCAT('Updated existing submenu permissions: ', ROW_COUNT()) AS 'Update Status';

-- Step 4: Verification summary
SELECT
    COUNT(*) AS 'Total Permissions',
    COUNT(DISTINCT smr.ser_sub_menu_id) AS 'Unique Submenus',
    SUM(CASE WHEN smr.bl_is_enabled = 1 THEN 1 ELSE 0 END) AS 'Enabled',
    SUM(CASE WHEN smr.bl_is_view = 1 THEN 1 ELSE 0 END) AS 'Can View',
    SUM(CASE WHEN smr.bl_is_add = 1 THEN 1 ELSE 0 END) AS 'Can Add',
    SUM(CASE WHEN smr.bl_is_update = 1 THEN 1 ELSE 0 END) AS 'Can Update',
    SUM(CASE WHEN smr.bl_is_delete = 1 THEN 1 ELSE 0 END) AS 'Can Delete',
    SUM(CASE WHEN smr.bl_is_approve = 1 THEN 1 ELSE 0 END) AS 'Can Approve'
FROM cfg_tbl_sub_menu_role smr
WHERE smr.ser_user_id = @target_user_id
  AND smr.bl_is_deleted = 0;

SELECT
    m.txt_menu_name AS 'Menu',
    sm.txt_sub_menu_name AS 'Submenu',
    smr.bl_is_enabled AS 'Enabled',
    smr.bl_is_view AS 'View',
    smr.bl_is_add AS 'Add',
    smr.bl_is_update AS 'Update',
    smr.bl_is_delete AS 'Delete',
    smr.bl_is_approve AS 'Approve'
FROM cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_sub_menu sm ON sm.ser_sub_menu_id = smr.ser_sub_menu_id
LEFT JOIN cfg_tbl_menu m ON m.ser_menu_id = sm.ser_menu_id
WHERE smr.ser_user_id = @target_user_id
  AND smr.bl_is_deleted = 0
ORDER BY m.txt_menu_name, sm.int_sub_menu_order, sm.txt_sub_menu_name;

SELECT
    CASE
        WHEN @target_user_name IS NULL THEN 'ERROR - user_id 343 not found'
        WHEN @target_role_id IS NULL THEN 'ERROR - role missing for user_id 343'
        WHEN EXISTS (
            SELECT 1
            FROM cfg_tbl_sub_menu sm
            WHERE (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
              AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
              AND NOT EXISTS (
                  SELECT 1
                  FROM cfg_tbl_sub_menu_role smr
                  WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
                    AND smr.ser_user_id = @target_user_id
                    AND smr.bl_is_deleted = 0
                    AND smr.bl_is_enabled = 1
              )
        ) THEN 'WARNING - Some active submenus are still missing/enabled=0'
        ELSE 'SUCCESS - user_id 343 has access to all active menus/submenus (logout/login required)'
    END AS 'Final Status';
