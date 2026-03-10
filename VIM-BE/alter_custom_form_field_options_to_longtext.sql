-- Ensure txt_field_options can store large JSON payloads (e.g., dynamic footer section users)
USE vim_3;

ALTER TABLE cfg_tbl_custom_form_field
MODIFY COLUMN txt_field_options LONGTEXT NULL;

