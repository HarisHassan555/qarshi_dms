-- =====================================================
-- Reusable workflow pipeline library (Digital Document Builder)
-- Stores pipeline step JSON (same shape as template payload.pipeline)
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_workflow_pipeline (
    ser_workflow_pipeline_id INT NOT NULL AUTO_INCREMENT,
    txt_name VARCHAR(200) NOT NULL COMMENT 'Display name e.g. Finance 3-level approval',
    txt_code VARCHAR(50) NULL COMMENT 'Optional unique short code e.g. WF-FIN-01',
    txt_description VARCHAR(500) NULL,
    txt_pipeline_type VARCHAR(32) NOT NULL DEFAULT 'STANDARD'
        COMMENT 'STANDARD = builder pipeline; future: INDIVIDUAL_FOOTER_TEMPLATE',
    txt_pipeline_json LONGTEXT NOT NULL
        COMMENT 'JSON array of pipeline steps (initiator + approver steps, fieldPermissions, users, etc.)',
    int_step_count INT NOT NULL DEFAULT 0 COMMENT 'Cached count of non-initiator steps (for list UI)',
    bl_is_active TINYINT(1) NOT NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user_id INT NULL,
    ser_modified_user_id INT NULL,
    PRIMARY KEY (ser_workflow_pipeline_id),
    UNIQUE KEY uk_workflow_pipeline_code (txt_code),
    KEY idx_workflow_pipeline_name (txt_name),
    KEY idx_workflow_pipeline_active (bl_is_active, bl_is_deleted, bln_status),
    KEY idx_workflow_pipeline_type (txt_pipeline_type),
    KEY idx_workflow_pipeline_modified (dte_modified_date),
    CONSTRAINT fk_workflow_pipeline_created_by
        FOREIGN KEY (ser_created_user_id) REFERENCES cfg_tbl_user (ser_user_id),
    CONSTRAINT fk_workflow_pipeline_modified_by
        FOREIGN KEY (ser_modified_user_id) REFERENCES cfg_tbl_user (ser_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional: link published template (custom form) to library pipeline (snapshot still in txt_template_payload)
-- Uncomment when backend uses this FK; safe to create now if you want the column ready.
/*
ALTER TABLE cfg_tbl_custom_form
    ADD COLUMN ser_workflow_pipeline_id INT NULL
        COMMENT 'Last linked library pipeline (snapshot remains in template JSON)'
        AFTER txt_approval_pipeline,
    ADD KEY idx_custom_form_workflow_pipeline (ser_workflow_pipeline_id),
    ADD CONSTRAINT fk_custom_form_workflow_pipeline
        FOREIGN KEY (ser_workflow_pipeline_id) REFERENCES cfg_tbl_workflow_pipeline (ser_workflow_pipeline_id);
*/

SELECT 'cfg_tbl_workflow_pipeline ready' AS message;
