-- Create or update seven ADMIN login users in the VIM auth table.
-- Target database: velocity_workbench
--
-- Login users:
--   user1 / 123
--   user2 / 123
--   user3 / 123
--   user4 / 123
--   user5 / 123
--   user6 / 123
--   user7 / 123
--
-- Notes:
-- - The backend authenticates against cfg_tbl_user.
-- - cfg_tbl_user has no separate employee_id column; ser_user_id is the app login/user id.
-- - This script uses random-looking user ids starting at 81001 unless that range is already occupied.
-- - It also grants full submenu permissions so the users behave like admins in the UI.

USE velocity_workbench;

SET @password_hash = '$2b$10$jx2bjar6ZLmcnrZyDlKa2OLBzZjMABAEXNWi.Zo52.QurLztCNj7K';
SET @email = 'harishassan551@gmail.com';

SET @admin_role_id = (
  SELECT ser_role_id
  FROM cfg_tbl_role
  WHERE UPPER(TRIM(txt_role_name)) = 'ADMIN'
    AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    AND (bl_is_active = 1 OR bl_is_active IS NULL)
  ORDER BY ser_role_id
  LIMIT 1
);

SET @password_policy_id = (
  SELECT ser_password_policy_id
  FROM cfg_tbl_password_policy
  WHERE (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
  ORDER BY ser_password_policy_id
  LIMIT 1
);

SELECT
  @admin_role_id AS admin_role_id,
  @password_policy_id AS password_policy_id,
  CASE
    WHEN @admin_role_id IS NULL THEN 'ERROR: ADMIN role not found. No users will be inserted.'
    ELSE 'OK: ADMIN role found.'
  END AS status;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_admin_users;
CREATE TEMPORARY TABLE tmp_seed_admin_users (
  username varchar(255) NOT NULL PRIMARY KEY,
  contact_no varchar(255) NOT NULL,
  row_no int NOT NULL
);

INSERT INTO tmp_seed_admin_users (username, contact_no, row_no) VALUES
  ('user1', '03005551017', 1),
  ('user2', '03125552984', 2),
  ('user3', '03215554309', 3),
  ('user4', '03335557216', 4),
  ('user5', '03455558631', 5),
  ('user6', '03015559442', 6),
  ('user7', '03175550738', 7);

SET @base_user_id = GREATEST(
  IFNULL((SELECT MAX(ser_user_id) + 1 FROM cfg_tbl_user WHERE ser_user_id > 0), 1),
  81001
);

INSERT INTO cfg_tbl_user (
  ser_user_id,
  txt_user_name,
  txt_address,
  txt_contact_no,
  txt_password,
  ser_role_id,
  txt_role,
  ser_password_policy_id,
  bl_is_active,
  bln_status,
  bl_is_deleted,
  bl_is_group_customer,
  bl_is_password_chang,
  num_attempt,
  num_sub_menu_order,
  dte_created_date,
  dte_expiry_date,
  ser_created_user,
  txt_department_name,
  txt_designation
)
SELECT
  @base_user_id + s.row_no - 1,
  s.username,
  @email,
  s.contact_no,
  @password_hash,
  @admin_role_id,
  'ADMIN',
  @password_policy_id,
  1,
  1,
  0,
  0,
  0,
  0,
  0,
  NOW(6),
  DATE_ADD(NOW(6), INTERVAL 365 DAY),
  1,
  'IT',
  'Admin'
FROM tmp_seed_admin_users s
WHERE @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_user u
    WHERE LOWER(TRIM(u.txt_user_name)) = s.username
  );

UPDATE cfg_tbl_user u
JOIN tmp_seed_admin_users s
  ON LOWER(TRIM(u.txt_user_name)) = s.username
SET
  u.txt_address = @email,
  u.txt_contact_no = s.contact_no,
  u.txt_password = @password_hash,
  u.ser_role_id = @admin_role_id,
  u.txt_role = 'ADMIN',
  u.ser_password_policy_id = @password_policy_id,
  u.bl_is_active = 1,
  u.bln_status = 1,
  u.bl_is_deleted = 0,
  u.bl_is_group_customer = 0,
  u.bl_is_password_chang = 0,
  u.num_attempt = 0,
  u.dte_modified_date = NOW(6),
  u.dte_expiry_date = DATE_ADD(NOW(6), INTERVAL 365 DAY),
  u.ser_modified_user = 1,
  u.txt_department_name = 'IT',
  u.txt_designation = 'Admin'
WHERE @admin_role_id IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_seed_admin_user_ids;
CREATE TEMPORARY TABLE tmp_seed_admin_user_ids AS
SELECT
  MIN(u.ser_user_id) AS ser_user_id,
  s.username
FROM tmp_seed_admin_users s
JOIN cfg_tbl_user u
  ON LOWER(TRIM(u.txt_user_name)) = s.username
GROUP BY s.username;

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
  NOW(6),
  1,
  @admin_role_id,
  f.ser_user_id
FROM (
  SELECT ser_user_id
  FROM tmp_seed_admin_user_ids
  ORDER BY ser_user_id
) f
WHERE @admin_role_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_user_role ur
    WHERE ur.ser_user_id = f.ser_user_id
      AND ur.ser_role_id = @admin_role_id
  );

UPDATE cfg_tbl_user_role ur
JOIN tmp_seed_admin_user_ids f
  ON f.ser_user_id = ur.ser_user_id
SET
  ur.bl_is_active = 1,
  ur.bln_status = 1,
  ur.ser_role_id = @admin_role_id,
  ur.dte_modified_date = NOW(6),
  ur.ser_modified_user = 1
WHERE @admin_role_id IS NOT NULL;

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
  bl_is_new_view,
  bl_is_new_update,
  dte_created_date,
  ser_created_user
)
SELECT
  sm.ser_sub_menu_id,
  @admin_role_id,
  f.ser_user_id,
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
  1,
  1,
  NOW(6),
  1
FROM cfg_tbl_sub_menu sm
JOIN tmp_seed_admin_user_ids f
WHERE @admin_role_id IS NOT NULL
  AND (sm.bl_is_deleted = 0 OR sm.bl_is_deleted IS NULL)
  AND (sm.bln_status = 1 OR sm.bln_status IS NULL)
  AND NOT EXISTS (
    SELECT 1
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_sub_menu_id = sm.ser_sub_menu_id
      AND smr.ser_user_id = f.ser_user_id
      AND smr.ser_role_id = @admin_role_id
  );

UPDATE cfg_tbl_sub_menu_role smr
JOIN tmp_seed_admin_user_ids f
  ON f.ser_user_id = smr.ser_user_id
SET
  smr.ser_role_id = @admin_role_id,
  smr.bl_is_active = 1,
  smr.bln_status = 1,
  smr.bl_is_deleted = 0,
  smr.bl_is_view = 1,
  smr.bl_is_add = 1,
  smr.bl_is_delete = 1,
  smr.bl_is_update = 1,
  smr.bl_is_approve = 1,
  smr.bl_is_enabled = 1,
  smr.bl_is_all = 1,
  smr.bl_is_create = 1,
  smr.bl_is_NewView = 1,
  smr.bl_is_NewUpdate = 1,
  smr.bl_is_new_view = 1,
  smr.bl_is_new_update = 1,
  smr.dte_modified_date = NOW(6),
  smr.ser_modified_user = 1
WHERE @admin_role_id IS NOT NULL;

SELECT
  u.ser_user_id AS user_id,
  u.txt_user_name AS username,
  u.txt_address AS email,
  u.txt_contact_no AS contact_no,
  u.txt_role AS role_name,
  u.ser_role_id AS role_id,
  LEFT(u.txt_password, 7) AS password_prefix,
  (
    SELECT COUNT(*)
    FROM cfg_tbl_user_role ur
    WHERE ur.ser_user_id = u.ser_user_id
      AND ur.ser_role_id = @admin_role_id
  ) AS admin_role_links,
  (
    SELECT COUNT(*)
    FROM cfg_tbl_sub_menu_role smr
    WHERE smr.ser_user_id = u.ser_user_id
      AND smr.ser_role_id = @admin_role_id
  ) AS submenu_permission_rows
FROM cfg_tbl_user u
JOIN tmp_seed_admin_user_ids f
  ON f.ser_user_id = u.ser_user_id
ORDER BY u.txt_user_name;

