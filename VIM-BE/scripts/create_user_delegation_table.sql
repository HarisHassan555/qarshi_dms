-- =====================================================
-- User delegation: who may act on behalf of whom (forms / documents)
-- Scope: multi-select stored as comma-separated codes in txt_scope
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

-- txt_scope: comma-separated published template form IDs (same id as Digital Document List / ser_form_id)
-- Example: '12,45,88'

CREATE TABLE IF NOT EXISTS cfg_tbl_user_delegation (
    ser_delegation_id INT NOT NULL AUTO_INCREMENT,
    ser_delegate_user_id INT NOT NULL COMMENT 'User who creates/submits on behalf of another',
    ser_on_behalf_user_id INT NOT NULL COMMENT 'User the action is attributed to (e.g. HOD)',
    dte_valid_from DATE NOT NULL,
    dte_valid_to DATE NOT NULL,
    txt_scope VARCHAR(512) NOT NULL COMMENT 'Comma-separated published document form IDs (template ser_form_id)',
    txt_notes VARCHAR(2000) NULL,
    txt_attachment_path VARCHAR(512) NULL COMMENT 'Relative path under ~/.vim_dms_uploads (user-delegations/)',
    txt_attachment_name VARCHAR(255) NULL COMMENT 'Original uploaded file name',
    txt_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, SCHEDULED, EXPIRED, REVOKED',
    dte_revoked_date DATETIME NULL,
    ser_revoked_by_user_id INT NULL,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL,
    ser_created_user_id INT NULL,
    ser_modified_user_id INT NULL,
    PRIMARY KEY (ser_delegation_id),
    KEY idx_delegation_delegate (ser_delegate_user_id),
    KEY idx_delegation_on_behalf (ser_on_behalf_user_id),
    KEY idx_delegation_valid_range (dte_valid_from, dte_valid_to),
    KEY idx_delegation_status (txt_status, bl_is_deleted),
    CONSTRAINT fk_delegation_delegate_user
        FOREIGN KEY (ser_delegate_user_id) REFERENCES cfg_tbl_user (ser_user_id),
    CONSTRAINT fk_delegation_on_behalf_user
        FOREIGN KEY (ser_on_behalf_user_id) REFERENCES cfg_tbl_user (ser_user_id),
    CONSTRAINT fk_delegation_created_by
        FOREIGN KEY (ser_created_user_id) REFERENCES cfg_tbl_user (ser_user_id),
    CONSTRAINT fk_delegation_modified_by
        FOREIGN KEY (ser_modified_user_id) REFERENCES cfg_tbl_user (ser_user_id),
    CONSTRAINT fk_delegation_revoked_by
        FOREIGN KEY (ser_revoked_by_user_id) REFERENCES cfg_tbl_user (ser_user_id),
    CONSTRAINT chk_delegation_date_range CHECK (dte_valid_to >= dte_valid_from),
    CONSTRAINT chk_delegation_distinct_users CHECK (ser_delegate_user_id <> ser_on_behalf_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional: prevent duplicate active rows for same triple (delegate, on-behalf, overlapping period)
-- Uncomment if you want DB-level uniqueness for non-deleted ACTIVE/SCHEDULED rows only (MySQL 8+ functional index workaround is limited).
-- CREATE UNIQUE INDEX uk_delegation_active_pair
--     ON cfg_tbl_user_delegation (ser_delegate_user_id, ser_on_behalf_user_id, dte_valid_from, dte_valid_to);

SELECT 'cfg_tbl_user_delegation ready' AS message;
