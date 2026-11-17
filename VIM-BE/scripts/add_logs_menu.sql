-- New top-level menu "Logs".
-- Moves Activity Logs, Email Delivery Log, and Storage under it.
-- Existing submenu role rows stay attached to the same submenu ids.

USE velocity;

SET @logs_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Logs'
    LIMIT 1
);

SET @next_menu_id = IFNULL((SELECT MAX(ser_menu_id) + 1 FROM cfg_tbl_menu), 1);

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
    @next_menu_id,
    'Logs',
    'glyphicon glyphicon-list-alt',
    1,
    1,
    0,
    NOW(),
    1
WHERE @logs_menu_id IS NULL;

SET @logs_menu_id = (
    SELECT ser_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Logs'
    LIMIT 1
);

UPDATE cfg_tbl_menu
SET
    txt_menu_name = 'Logs',
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW()
WHERE ser_menu_id = @logs_menu_id
  AND @logs_menu_id IS NOT NULL;

SET @activity_logs_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE LOWER(TRIM(BOTH '/' FROM txt_sub_menu_url)) = 'activitylogs'
    ORDER BY ser_sub_menu_id
    LIMIT 1
);

SET @email_delivery_log_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE LOWER(TRIM(BOTH '/' FROM txt_sub_menu_url)) = 'email-delivery-log'
    ORDER BY ser_sub_menu_id
    LIMIT 1
);

SET @storage_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE LOWER(TRIM(BOTH '/' FROM txt_sub_menu_url)) = 'application-storage'
    ORDER BY ser_sub_menu_id
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @logs_menu_id,
    int_sub_menu_order = 1,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @activity_logs_id
  AND @logs_menu_id IS NOT NULL
  AND @activity_logs_id IS NOT NULL;

UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @logs_menu_id,
    int_sub_menu_order = 2,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @email_delivery_log_id
  AND @logs_menu_id IS NOT NULL
  AND @email_delivery_log_id IS NOT NULL;

UPDATE cfg_tbl_sub_menu
SET
    ser_menu_id = @logs_menu_id,
    int_sub_menu_order = 3,
    bl_is_active = 1,
    bln_status = 1,
    bl_is_deleted = 0,
    dte_modified_date = NOW()
WHERE ser_sub_menu_id = @storage_id
  AND @logs_menu_id IS NOT NULL
  AND @storage_id IS NOT NULL;

SELECT m.ser_menu_id, m.txt_menu_name, sm.ser_sub_menu_id, sm.txt_sub_menu_name, sm.txt_sub_menu_url, sm.int_sub_menu_order
FROM cfg_tbl_menu m
JOIN cfg_tbl_sub_menu sm ON sm.ser_menu_id = m.ser_menu_id
WHERE m.txt_menu_name = 'Logs'
ORDER BY sm.int_sub_menu_order;
