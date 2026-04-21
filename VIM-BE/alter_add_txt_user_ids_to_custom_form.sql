-- Add user assignment storage for form builder
-- Run once on the target database.
ALTER TABLE cfg_tbl_custom_form
ADD COLUMN txt_user_ids TEXT NULL;

