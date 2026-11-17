-- Storage page under Master Data
-- Angular route: application-storage  →  /velocity/application-storage
-- View permission for ADMIN roles

USE velocity;

SET @velocity_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Master Data'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
      AND (bln_status = 1 OR bln_status IS NULL)
    LIMIT 1
);

SET @storage_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
      AND LOWER(TRIM(BOTH '/' FROM txt_sub_menu_url)) = 'application-storage'
    LIMIT 1
);

SET @next_order = IFNULL((
    SELECT MAX(int_sub_menu_order) + 1
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
), 50);

SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);

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
    @next_submenu_id,
    @velocity_menu_id,
    'Storage',
    'application-storage',
    @next_order,
    1, 1, 0,
    1, 0, 0, 0, 0,
    NOW(),
    1
WHERE @velocity_menu_id IS NOT NULL
  AND @storage_submenu_id IS NULL;

SET @storage_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE ser_menu_id = @velocity_menu_id
      AND LOWER(TRIM(BOTH '/' FROM txt_sub_menu_url)) = 'application-storage'
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @velocity_menu_id,
    txt_sub_menu_name = 'Storage',
    txt_sub_menu_url = 'application-storage',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    bl_is_view = 1,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @storage_submenu_id
  AND @storage_submenu_id IS NOT NULL;

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
    dte_created_date,
    ser_created_user
)
SELECT
    @storage_submenu_id,
    r.ser_role_id,
    1, 1, 0,
    1, 0, 0, 0, 0,
    CASE WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
    0,
    NOW(),
    1
FROM cfg_tbl_role r
WHERE (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
  AND (r.bln_status = 1 OR r.bln_status IS NULL)
  AND @storage_submenu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM cfg_tbl_sub_menu_role smr
      WHERE smr.ser_sub_menu_id = @storage_submenu_id
        AND smr.ser_role_id = r.ser_role_id
        AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  );

UPDATE cfg_tbl_sub_menu_role smr
INNER JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
SET
    smr.bl_is_enabled = 1,
    smr.bl_is_view = 1,
    smr.bl_is_active = 1,
    smr.bln_status = 1,
    smr.bl_is_deleted = 0,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @storage_submenu_id
  AND @storage_submenu_id IS NOT NULL
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  AND UPPER(r.txt_role_name) LIKE '%ADMIN%';

SELECT @velocity_menu_id AS velocity_menu_id, @storage_submenu_id AS application_storage_submenu_id;
