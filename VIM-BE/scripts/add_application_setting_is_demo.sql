USE velocity;

ALTER TABLE cfg_tbl_application_setting
    ADD COLUMN bl_is_demo BIT(1) NOT NULL DEFAULT 0 AFTER bl_is_active;
