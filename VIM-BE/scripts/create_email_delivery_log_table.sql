-- =====================================================
-- Audit log: each triggered email to each recipient
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_email_delivery_log (
    ser_email_delivery_log_id BIGINT NOT NULL AUTO_INCREMENT,
    txt_trigger_type VARCHAR(64) NOT NULL DEFAULT 'SYSTEM',
    txt_template_code VARCHAR(64) NULL,
    ser_application_id INT NULL,
    ser_recipient_user_id INT NULL,
    txt_recipient_email VARCHAR(255) NOT NULL,
    txt_subject VARCHAR(512) NULL,
    txt_body_html LONGTEXT NULL,
    txt_body_text LONGTEXT NULL,
    txt_status VARCHAR(32) NOT NULL COMMENT 'SENT, FAILED, SKIPPED',
    txt_error_message LONGTEXT NULL,
    bln_has_attachments TINYINT(1) NOT NULL DEFAULT 0,
    ser_triggered_by_user_id INT NULL,
    ser_background_job_id BIGINT NULL,
    txt_context_label VARCHAR(255) NULL,
    dte_sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ser_email_delivery_log_id),
    KEY idx_email_log_sent_at (dte_sent_at),
    KEY idx_email_log_recipient (txt_recipient_email),
    KEY idx_email_log_status (txt_status),
    KEY idx_email_log_trigger (txt_trigger_type),
    KEY idx_email_log_application (ser_application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'cfg_tbl_email_delivery_log ready' AS message;