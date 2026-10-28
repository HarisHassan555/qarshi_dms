-- =====================================================
-- Form routing / threshold rules (submit-time field conditions)
-- Database: velocity
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_workflow_threshold_rule (
    ser_workflow_threshold_rule_id INT NOT NULL AUTO_INCREMENT,
    ser_form_id INT NOT NULL COMMENT 'cfg_tbl_custom_form.ser_form_id',
    txt_rule_name VARCHAR(200) NOT NULL,
    txt_field_key VARCHAR(120) NOT NULL,
    txt_field_label VARCHAR(200) NULL,
    txt_operator VARCHAR(32) NOT NULL COMMENT 'equals, greater_than, etc.',
    txt_compare_value VARCHAR(500) NOT NULL,
    txt_routing_action VARCHAR(16) NOT NULL DEFAULT 'override' COMMENT 'override | insert',
    txt_insert_position VARCHAR(16) NULL COMMENT 'before | after when insert',
    int_approval_step_order INT NOT NULL COMMENT 'Override step or insert anchor step',
    txt_target_type VARCHAR(16) NOT NULL COMMENT 'department | role | individual',
    ser_target_id INT NULL COMMENT 'Department, role, or user id',
    txt_target_label VARCHAR(200) NULL,
    int_priority INT NOT NULL DEFAULT 10 COMMENT 'Lower runs first',
    txt_notes VARCHAR(500) NULL,
    bl_is_active TINYINT(1) NOT NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user_id INT NULL,
    ser_modified_user_id INT NULL,
    PRIMARY KEY (ser_workflow_threshold_rule_id),
    KEY idx_wf_threshold_rule_form (ser_form_id, bl_is_deleted, bl_is_active),
    KEY idx_wf_threshold_rule_priority (ser_form_id, int_priority),
    CONSTRAINT fk_wf_threshold_rule_form
        FOREIGN KEY (ser_form_id) REFERENCES cfg_tbl_custom_form (ser_form_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'cfg_tbl_workflow_threshold_rule ready' AS message;
