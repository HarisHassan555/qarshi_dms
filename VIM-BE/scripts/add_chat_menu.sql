-- Optional: add Messages submenu. Adjust menu parent / DB name as needed.

USE velocity_workbench;

SET @velocity_menu_id = (
    SELECT ser_menu_id FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Velocity' AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1
);

SET @next_submenu_id = IFNULL((SELECT MAX(ser_sub_menu_id) + 1 FROM cfg_tbl_sub_menu), 1);
SET @next_order = IFNULL((SELECT MAX(int_sub_menu_order) + 1 FROM cfg_tbl_sub_menu WHERE ser_menu_id = @velocity_menu_id), 1);

INSERT INTO cfg_tbl_sub_menu (
    ser_sub_menu_id, ser_menu_id, txt_sub_menu_name, txt_sub_menu_url,
    int_sub_menu_order, bl_is_active, bl_is_deleted
)
SELECT @next_submenu_id, @velocity_menu_id, 'Messages', 'chat', @next_order, 1, 0
FROM DUAL
WHERE @velocity_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM cfg_tbl_sub_menu
      WHERE ser_menu_id = @velocity_menu_id AND txt_sub_menu_url = 'chat'
  );
