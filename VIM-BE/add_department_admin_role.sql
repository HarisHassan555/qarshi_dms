USE vim_3;

SET @department_admin_role_id = (
    SELECT ser_role_id
    FROM cfg_tbl_role
    WHERE UPPER(TRIM(txt_role_name)) = 'DEPARTMENT_ADMIN'
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
    'department_admin',
    'DEPARTMENT_ADMIN',
    1,
    1,
    0,
    NOW(),
    1
WHERE @department_admin_role_id IS NULL;

SET @department_admin_role_id = IFNULL(@department_admin_role_id, @next_role_id);

SET @department_application_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE txt_sub_menu_url = 'department-application'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    ORDER BY ser_sub_menu_id
    LIMIT 1
);

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
    @department_application_submenu_id,
    @department_admin_role_id,
    NULL,
    1,
    1,
    0,
    1,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    1,
    0,
    NOW(),
    1
WHERE @department_application_submenu_id IS NOT NULL
  AND @department_admin_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @department_application_submenu_id
        AND smr.ser_role_id = @department_admin_role_id
        AND smr.ser_user_id IS NULL
  );

UPDATE cfg_tbl_sub_menu_role
SET
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 0,
    bl_is_delete = 0,
    bl_is_update = 0,
    bl_is_approve = 0,
    bl_is_enabled = 1,
    bl_is_all = 0,
    bl_is_create = 0,
    bl_is_NewView = 1,
    bl_is_NewUpdate = 0,
    dte_modified_date = NOW(),
    ser_modified_user = 1
WHERE ser_sub_menu_id = @department_application_submenu_id
  AND ser_role_id = @department_admin_role_id
  AND ser_user_id IS NULL;
