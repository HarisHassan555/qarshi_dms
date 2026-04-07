-- Performance indexes for high-traffic application listing endpoints
-- Run in a maintenance window and validate with EXPLAIN before/after.

CREATE INDEX idx_cfg_tbl_custom_form_app_submitter_deleted_created
ON cfg_tbl_custom_form_application (ser_submitted_by, bl_is_deleted, dte_created_date);

CREATE INDEX idx_cfg_tbl_custom_form_app_status_approver_deleted_created
ON cfg_tbl_custom_form_application (txt_status, ser_current_approver, bl_is_deleted, dte_created_date);

