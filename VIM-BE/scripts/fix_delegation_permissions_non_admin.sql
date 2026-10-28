-- Turn off Delegation Management menu/API grants for roles that are not enabled.
-- Safe to run after menu seed; does not change rows where bl_is_enabled = 1.
-- Database: velocity

USE velocity;

SET @delegation_submenu_id = (
    SELECT ser_sub_menu_id
    FROM cfg_tbl_sub_menu
    WHERE LOWER(txt_sub_menu_url) = 'delegation-management'
    LIMIT 1
);

UPDATE cfg_tbl_sub_menu_role smr
JOIN cfg_tbl_role r ON r.ser_role_id = smr.ser_role_id
SET
    smr.bl_is_view = 0,
    smr.bl_is_NewView = 0,
    smr.bl_is_add = 0,
    smr.bl_is_create = 0,
    smr.bl_is_delete = 0,
    smr.bl_is_update = 0,
    smr.bl_is_NewUpdate = 0,
    smr.dte_modified_date = NOW()
WHERE smr.ser_sub_menu_id = @delegation_submenu_id
  AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
  AND (smr.bl_is_enabled IS NULL OR smr.bl_is_enabled = 0)
  AND UPPER(COALESCE(r.txt_role_name, '')) NOT LIKE '%ADMIN%';

SELECT ROW_COUNT() AS rows_updated;
