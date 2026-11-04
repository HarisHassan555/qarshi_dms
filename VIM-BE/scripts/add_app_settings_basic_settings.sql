USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_application_setting (
    ser_application_setting_id INT NOT NULL AUTO_INCREMENT,
    txt_application_name VARCHAR(200) NOT NULL,
    txt_address VARCHAR(500) NULL,
    txt_email VARCHAR(200) NULL,
    txt_logo_path VARCHAR(500) NULL,
    txt_dashboard_hero_image_path VARCHAR(500) NULL,
    txt_smtp_host VARCHAR(200) NULL,
    txt_smtp_port INT NULL,
    txt_smtp_username VARCHAR(200) NULL,
    txt_smtp_password VARCHAR(255) NULL,
    bl_smtp_auth BIT(1) NULL,
    bl_smtp_ssl_enable BIT(1) NULL,
    bl_smtp_starttls_enable BIT(1) NULL,
    bl_is_active BIT(1) NULL DEFAULT 1,
    bl_is_deleted BIT(1) NULL DEFAULT 0,
    ser_created_user INT NULL,
    dte_created_date DATETIME NULL,
    ser_modified_user INT NULL,
    dte_modified_date DATETIME NULL,
    PRIMARY KEY (ser_application_setting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'App Settings'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @new_menu_id = (SELECT IFNULL(MAX(ser_menu_id), 0) + 1 FROM cfg_tbl_menu);

INSERT INTO cfg_tbl_menu (
    ser_menu_id,
    txt_menu_name,
    txt_menu_icons,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    dte_created_date,
    ser_created_user
)
SELECT
    @new_menu_id,
    'App Settings',
    'settings',
    1, 1, 0,
    NOW(),
    1
WHERE @menu_id IS NULL;

SET @menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'App Settings'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

UPDATE cfg_tbl_menu
SET
    txt_menu_name = 'App Settings',
    txt_menu_icons = 'settings',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW()
WHERE ser_menu_id = @menu_id;

SET @submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @menu_id
      AND txt_sub_menu_name = 'Basic Settings'
    LIMIT 1
);

SET @new_submenu_id = (SELECT IFNULL(MAX(ser_sub_menu_id), 0) + 1 FROM cfg_tbl_sub_menu);

INSERT INTO cfg_tbl_sub_menu (
    ser_sub_menu_id,
    ser_menu_id,
    txt_sub_menu_name,
    txt_sub_menu_url,
    int_sub_menu_order,
    bl_is_active,
    bln_status,
    bl_is_deleted,
    bl_is_view,
    bl_is_add,
    bl_is_delete,
    bl_is_update,
    bl_is_approve,
    dte_created_date,
    ser_created_user
)
SELECT
    @new_submenu_id,
    @menu_id,
    'Basic Settings',
    'basic-settings',
    1,
    1, 1, 0,
    1, 1, 1, 1, 0,
    NOW(),
    1
WHERE @menu_id IS NOT NULL
  AND @submenu_id IS NULL;

SET @submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @menu_id
      AND txt_sub_menu_name = 'Basic Settings'
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    txt_sub_menu_name = 'Basic Settings',
    txt_sub_menu_url = 'basic-settings',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    bl_is_add = 1,
    bl_is_update = 1,
    bl_is_delete = 1,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @submenu_id;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_admin_roles (
    ser_role_id INT PRIMARY KEY
);

DELETE FROM tmp_admin_roles WHERE ser_role_id IS NOT NULL;

INSERT INTO tmp_admin_roles (ser_role_id)
SELECT r.ser_role_id
FROM cfg_tbl_role r
WHERE UPPER(r.txt_role_name) LIKE '%ADMIN%'
  AND (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL);

INSERT INTO cfg_tbl_sub_menu_role (
    ser_sub_menu_id,
    ser_role_id,
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
    @submenu_id,
    ar.ser_role_id,
    1, 1, 0,
    1, 1, 1, 1, 0,
    1, 0,
    1, 1, 1,
    NOW(),
    1
FROM tmp_admin_roles ar
WHERE @submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @submenu_id
        AND smr.ser_role_id = ar.ser_role_id
        AND smr.ser_user_id IS NULL
  );

UPDATE cfg_tbl_sub_menu_role smr
JOIN tmp_admin_roles ar ON ar.ser_role_id = smr.ser_role_id
SET
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.bl_is_view = 1,
    smr.bl_is_add = 1,
    smr.bl_is_delete = 1,
    smr.bl_is_update = 1,
    smr.bl_is_enabled = 1,
    smr.bl_is_create = 1,
    smr.bl_is_NewView = 1,
    smr.bl_is_NewUpdate = 1,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @submenu_id
  AND smr.ser_user_id IS NULL;

SELECT @menu_id AS menu_id, @submenu_id AS submenu_id;
