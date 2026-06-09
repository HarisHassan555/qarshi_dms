-- =============================================================================
-- Grant CEO role full menu/submenu access so CEO users can log in and work.
--
-- Problem: After login the sidebar loads role-level permissions (ser_user_id IS
-- NULL). If the CEO role has no enabled submenu rows, the UI logs the user out
-- with "You do not have access to the application."
--
-- This script:
--   1) Ensures a CEO role exists in cfg_tbl_role
--   2) Normalises all CEO users (active, correct ser_role_id + txt_role)
--   3) Links CEO users in cfg_tbl_user_role
--   4) Grants role-level + user-specific submenu permissions
--   5) Optionally resets CEO passwords to 123 (BCrypt) — set @reset_password = 0 to skip
--
-- Run:
--   mysql -h 127.0.0.1 -P 3308 -u root -p velocity_workbench < grant_ceo_role_permissions.sql
-- =============================================================================

USE velocity_workbench;

SET @old_sql_safe_updates = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

-- Set to 1 to reset all CEO-user passwords to plain '123' (BCrypt hash below)
SET @reset_password = 1;
SET @password_hash = '$2b$10$jx2bjar6ZLmcnrZyDlKa2OLBzZjMABAEXNWi.Zo52.QurLztCNj7K';

-- ---------------------------------------------------------------------------
-- 1) Ensure CEO role exists
-- ---------------------------------------------------------------------------
SET @ceo_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_role
    WHERE UPPER(TRIM(txt_role_name)) = 'CEO'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY ser_role_id
    LIMIT 1
);

SET @next_role_id = IFNULL((SELECT MAX(ser_role_id) + 1 FROM cfg_tbl_role), 1);

INSERT INTO cfg_tbl_role (
    ser_role_id,
    txt_role_name,
    txt_role_code,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    dte_created_date,
    ser_created_user
)
SELECT
    @next_role_id,
    'CEO',
    'CEO',
    1,
    1,
    0,
    NOW(),
    1
WHERE @ceo_role_id IS NULL;

SET @ceo_role_id = IFNULL(@ceo_role_id, @next_role_id);

SELECT
    @ceo_role_id AS ceo_role_id,
    CASE
        WHEN @ceo_role_id IS NULL THEN 'STOP - CEO role could not be resolved'
        ELSE 'OK - CEO role ready'
    END AS role_status;

-- ---------------------------------------------------------------------------
-- 2) Normalise CEO users (by role link or txt_role)
-- ---------------------------------------------------------------------------
UPDATE cfg_tbl_user u
LEFT JOIN cfg_tbl_role r ON r.ser_role_id = u.ser_role_id
SET
    u.ser_role_id = @ceo_role_id,
    u.txt_role = 'CEO',
    u.bl_is_active = 1,
    u.bln_status = 1,
    u.bl_is_deleted = 0,
    u.num_attempt = 0,
    u.dte_modified_date = NOW(),
    u.ser_modified_user = 1
WHERE @ceo_role_id IS NOT NULL
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
  AND (
      UPPER(TRIM(IFNULL(u.txt_role, ''))) = 'CEO'
      OR UPPER(TRIM(IFNULL(r.txt_role_name, ''))) = 'CEO'
  );

UPDATE cfg_tbl_user u
SET
    u.txt_password = @password_hash,
    u.dte_modified_date = NOW(),
    u.ser_modified_user = 1
WHERE @reset_password = 1
  AND @ceo_role_id IS NOT NULL
  AND u.ser_role_id = @ceo_role_id
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL);

-- ---------------------------------------------------------------------------
-- 3) Ensure cfg_tbl_user_role links for CEO users
-- ---------------------------------------------------------------------------
SET @next_user_role_id = IFNULL((SELECT MAX(ser_user_role_id) FROM cfg_tbl_user_role), 0);

INSERT INTO cfg_tbl_user_role (
    ser_user_role_id,
    bl_is_active,
    bln_status,
    dte_created_date,
    ser_created_user,
    ser_role_id,
    ser_user_id
)
SELECT
    (@next_user_role_id := @next_user_role_id + 1),
    1,
    1,
    NOW(),
    1,
    @ceo_role_id,
    u.ser_user_id
FROM cfg_tbl_user u
WHERE @ceo_role_id IS NOT NULL
  AND u.ser_role_id = @ceo_role_id
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_user_role ur
      WHERE ur.ser_user_id = u.ser_user_id
        AND ur.ser_role_id = @ceo_role_id
  );

UPDATE cfg_tbl_user_role ur
JOIN cfg_tbl_user u ON u.ser_user_id = ur.ser_user_id
SET
    ur.ser_role_id = @ceo_role_id,
    ur.bl_is_active = 1,
    ur.bln_status = 1,
    ur.dte_modified_date = NOW(),
    ur.ser_modified_user = 1
WHERE @ceo_role_id IS NOT NULL
  AND u.ser_role_id = @ceo_role_id
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL);

-- ---------------------------------------------------------------------------
-- 4) Role-level submenu permissions (ser_user_id IS NULL) — required by sidebar
-- ---------------------------------------------------------------------------
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
    @ceo_role_id,
    NULL,
    1,
    1,
    0,
    1,
    1,
    0,
    1,
    1,
    1,
    0,
    1,
    1,
    1,
    NOW(),
    1
FROM cfg_tbl_sub_menu sm
WHERE @ceo_role_id IS NOT NULL
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
        AND smr.ser_role_id = @ceo_role_id
        AND smr.ser_user_id IS NULL
  );

UPDATE cfg_tbl_sub_menu_role smr
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_delete = 0,
    bl_is_update = 1,
    bl_is_approve = 1,
    bl_is_enabled = 1,
    bl_is_all = 0,
    bl_is_create = 1,
    bl_is_NewView = 1,
    bl_is_NewUpdate = 1,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE smr.ser_role_id = @ceo_role_id
  AND smr.ser_user_id IS NULL
  AND @ceo_role_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 5) User-specific submenu permissions (belt-and-braces for API checks)
-- ---------------------------------------------------------------------------
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
    @ceo_role_id,
    u.ser_user_id,
    1,
    1,
    0,
    1,
    1,
    0,
    1,
    1,
    1,
    0,
    1,
    1,
    1,
    NOW(),
    1
FROM cfg_tbl_sub_menu sm
JOIN cfg_tbl_user u ON u.ser_role_id = @ceo_role_id
WHERE @ceo_role_id IS NOT NULL
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
        AND smr.ser_role_id = @ceo_role_id
        AND smr.ser_user_id = u.ser_user_id
  );

UPDATE cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_user u ON u.ser_user_id = smr.ser_user_id
SET
    smr.ser_role_id = @ceo_role_id,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.bl_is_view = 1,
    smr.bl_is_add = 1,
    smr.bl_is_delete = 0,
    smr.bl_is_update = 1,
    smr.bl_is_approve = 1,
    smr.bl_is_enabled = 1,
    smr.bl_is_all = 0,
    smr.bl_is_create = 1,
    smr.bl_is_NewView = 1,
    smr.bl_is_NewUpdate = 1,
    smr.dte_modified_date = NOW(),
    smr.ser_modified_user = 1
WHERE @ceo_role_id IS NOT NULL
  AND u.ser_role_id = @ceo_role_id
  AND smr.ser_user_id = u.ser_user_id;

SET SQL_SAFE_UPDATES = @old_sql_safe_updates;

-- ---------------------------------------------------------------------------
-- 6) Verification
-- ---------------------------------------------------------------------------
SELECT
    u.ser_user_id AS user_id,
    u.txt_user_name AS username,
    u.txt_role AS txt_role,
    u.ser_role_id AS role_id,
    u.bln_status AS active_status,
    u.bl_is_active AS is_active,
    LEFT(u.txt_password, 7) AS password_prefix
FROM cfg_tbl_user u
WHERE u.ser_role_id = @ceo_role_id
  AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
ORDER BY u.ser_user_id;

SELECT
    COUNT(*) AS role_level_permissions,
    SUM(CASE WHEN smr.bl_is_enabled = 1 THEN 1 ELSE 0 END) AS enabled_permissions
FROM cfg_tbl_sub_menu_role smr
WHERE smr.ser_role_id = @ceo_role_id
  AND smr.ser_user_id IS NULL
  AND smr.bl_is_deleted = 0;

SELECT
    sm.txt_sub_menu_name AS submenu,
    sm.txt_sub_menu_url AS url,
    smr.bl_is_enabled AS enabled,
    smr.bl_is_view AS can_view,
    smr.bl_is_approve AS can_approve
FROM cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_sub_menu sm ON sm.ser_sub_menu_id = smr.ser_sub_menu_id
WHERE smr.ser_role_id = @ceo_role_id
  AND smr.ser_user_id IS NULL
  AND smr.bl_is_deleted = 0
  AND sm.txt_sub_menu_name IN (
      'Pending Approvals',
      'Applications View',
      'Application',
      'CAPF',
      'Signature',
      'Change Password'
  )
ORDER BY sm.txt_sub_menu_name;

SELECT
    CASE
        WHEN @ceo_role_id IS NULL THEN 'FAILED - CEO role missing'
        WHEN NOT EXISTS (
            SELECT 1
            FROM cfg_tbl_user u
            WHERE u.ser_role_id = @ceo_role_id
              AND (u.bl_is_deleted = 0 OR u.bl_is_deleted IS NULL)
        ) THEN 'WARNING - CEO role exists but no CEO users found'
        WHEN NOT EXISTS (
            SELECT 1
            FROM cfg_tbl_sub_menu_role smr
            WHERE smr.ser_role_id = @ceo_role_id
              AND smr.ser_user_id IS NULL
              AND smr.bl_is_deleted = 0
              AND smr.bl_is_enabled = 1
        ) THEN 'FAILED - No enabled role-level submenu permissions for CEO'
        ELSE CONCAT(
            'SUCCESS - CEO role (', @ceo_role_id, ') permissions granted. ',
            'Logout/login required',
            CASE WHEN @reset_password = 1 THEN ' — CEO passwords set to 123' ELSE '' END
        )
    END AS final_status;
