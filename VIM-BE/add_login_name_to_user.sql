ALTER TABLE cfg_tbl_user
ADD COLUMN txt_login_name VARCHAR(255) NULL;

CREATE UNIQUE INDEX uq_cfg_tbl_user_txt_login_name
ON cfg_tbl_user (txt_login_name);
