-- =====================================================
-- Durable background / queue jobs (email, PDF, SAP sync, etc.)
-- Worker polls rows where txt_status = 'PENDING' and dte_run_after <= NOW()
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_background_job (
    ser_background_job_id BIGINT NOT NULL AUTO_INCREMENT,
    txt_job_type VARCHAR(64) NOT NULL COMMENT 'e.g. TEMPLATE_POST_APPROVAL_EMAIL, SAP_HCM_EMPLOYEE_SYNC',
    txt_idempotency_key VARCHAR(191) NOT NULL COMMENT 'Unique per logical event; prevents duplicate enqueue',
    txt_payload LONGTEXT NULL COMMENT 'JSON job parameters',
    txt_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, RUNNING, DONE, FAILED, CANCELLED',
    int_attempts INT NOT NULL DEFAULT 0 COMMENT 'Completed execution attempts (increment on each run)',
    int_max_attempts INT NOT NULL DEFAULT 5,
    dte_run_after DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Do not run before this time (backoff / schedule)',
    dte_locked_at DATETIME NULL COMMENT 'Set when a worker claims the row',
    txt_locked_by VARCHAR(128) NULL COMMENT 'Worker instance id (hostname:pid or configured name)',
    txt_last_error LONGTEXT NULL,
    dte_started_at DATETIME NULL COMMENT 'Latest run start',
    dte_completed_at DATETIME NULL COMMENT 'Set when DONE or terminal FAILED/CANCELLED',
    ser_created_user_id INT NULL COMMENT 'User who triggered enqueue, if applicable',
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ser_background_job_id),
    UNIQUE KEY uk_background_job_idempotency (txt_idempotency_key),
    KEY idx_background_job_poll (txt_status, dte_run_after, ser_background_job_id),
    KEY idx_background_job_type_status (txt_job_type, txt_status),
    KEY idx_background_job_created (dte_created_date),
    KEY idx_background_job_completed (dte_completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Planned txt_job_type values (application enforces; not a DB enum):
--   TEMPLATE_POST_APPROVAL_EMAIL
--   TEMPLATE_PDF_REFRESH_BEFORE_EMAIL
--   TEMPLATE_POST_APPROVAL_EMAIL_WITH_PDF
--   TEMPLATE_PDF_ATTACH
--   SAP_HCM_EMPLOYEE_SYNC
--   SAP_HCM_EMPLOYEE_XML_SYNC
--   APPROVAL_REMINDER_BATCH
--   USER_CREDENTIAL_EMAIL
--   CHANGE_PASSWORD_EMAIL
--   DELEGATION_NOTIFY_EMAIL

SELECT 'cfg_tbl_background_job ready' AS message;
