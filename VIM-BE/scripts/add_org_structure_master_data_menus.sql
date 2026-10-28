-- Add Organizations, Sub Areas, Divisions under Master Data
USE velocity;

DROP PROCEDURE IF EXISTS add_master_data_submenu;
DELIMITER //
CREATE PROCEDURE add_master_data_submenu(
    IN p_name VARCHAR(128),
    IN p_url VARCHAR(128)
)
BEGIN
    DECLARE v_menu_id INT;
    DECLARE v_submenu_id INT;
    DECLARE v_next_order INT;
    DECLARE v_next_id INT;

    SELECT ser_menu_id INTO v_menu_id
    FROM cfg_tbl_menu
    WHERE txt_menu_name = 'Master Data'
      AND (bl_is_deleted = 0 OR bl_is_deleted IS NULL)
    LIMIT 1;

    IF v_menu_id IS NULL THEN
        SELECT CONCAT('Master Data menu not found; skipped ', p_name) AS message;
    ELSE
        SELECT ser_sub_menu_id INTO v_submenu_id
        FROM cfg_tbl_sub_menu
        WHERE ser_menu_id = v_menu_id AND txt_sub_menu_name = p_name
        LIMIT 1;

        SELECT IFNULL(MAX(int_sub_menu_order) + 1, 1) INTO v_next_order
        FROM cfg_tbl_sub_menu WHERE ser_menu_id = v_menu_id;

        SELECT IFNULL(MAX(ser_sub_menu_id) + 1, 1) INTO v_next_id FROM cfg_tbl_sub_menu;

        IF v_submenu_id IS NULL THEN
            INSERT INTO cfg_tbl_sub_menu (
                ser_sub_menu_id, ser_menu_id, txt_sub_menu_name, txt_sub_menu_url,
                int_sub_menu_order, bl_is_active, bln_status, bl_is_deleted,
                bl_is_view, bl_is_add, bl_is_delete, bl_is_update, bl_is_approve,
                dte_created_date, ser_created_user
            ) VALUES (
                v_next_id, v_menu_id, p_name, p_url, v_next_order,
                1, 1, 0, 1, 0, 0, 0, 0, NOW(), 1
            );
            SET v_submenu_id = v_next_id;
        ELSE
            UPDATE cfg_tbl_sub_menu
            SET txt_sub_menu_url = p_url, bl_is_active = 1, bln_status = 1, bl_is_deleted = 0,
                bl_is_view = 1, dte_modified_date = NOW()
            WHERE ser_sub_menu_id = v_submenu_id;
        END IF;

        INSERT INTO cfg_tbl_sub_menu_role (
            ser_sub_menu_id, ser_role_id, bl_is_active, bln_status, bl_is_deleted,
            bl_is_view, bl_is_add, bl_is_delete, bl_is_update, bl_is_approve,
            bl_is_enabled, bl_is_all, dte_created_date, ser_created_user
        )
        SELECT v_submenu_id, r.ser_role_id, 1, 1, 0, 1, 0, 0, 0, 0,
               CASE WHEN UPPER(r.txt_role_name) LIKE '%ADMIN%' THEN 1 ELSE 0 END,
               0, NOW(), 1
        FROM cfg_tbl_role r
        WHERE (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)
          AND (r.bln_status = 1 OR r.bln_status IS NULL)
          AND NOT EXISTS (
              SELECT 1 FROM cfg_tbl_sub_menu_role smr
              WHERE smr.ser_sub_menu_id = v_submenu_id
                AND smr.ser_role_id = r.ser_role_id
                AND (smr.ser_user_id IS NULL OR smr.ser_user_id = 0)
          );
    END IF;
END //
DELIMITER ;

CALL add_master_data_submenu('Organizations', 'organizations');
CALL add_master_data_submenu('Sub Areas', 'sub-areas');
CALL add_master_data_submenu('Divisions', 'divisions');

DROP PROCEDURE IF EXISTS add_master_data_submenu;
