-- =====================================================
-- Email template module: shared layouts + message templates
-- Placeholders in subject/body: {{loginName}}, {{displayName}}, etc.
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

-- Optional shared wrapper (header/footer). Body templates inject into {{content}} when layout is used.
CREATE TABLE IF NOT EXISTS cfg_tbl_email_layout (
    ser_email_layout_id INT NOT NULL AUTO_INCREMENT,
    txt_layout_code VARCHAR(64) NOT NULL COMMENT 'Stable code e.g. DEFAULT_QARSHI',
    txt_layout_name VARCHAR(200) NOT NULL,
    txt_description VARCHAR(500) NULL,
    txt_html_wrapper LONGTEXT NOT NULL COMMENT 'Full HTML; include {{content}} where body is inserted',
    bl_is_active TINYINT(1) NOT NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user_id INT NULL,
    ser_modified_user_id INT NULL,
    PRIMARY KEY (ser_email_layout_id),
    UNIQUE KEY uk_email_layout_code (txt_layout_code),
    KEY idx_email_layout_active (bl_is_active, bl_is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cfg_tbl_email_template (
    ser_email_template_id INT NOT NULL AUTO_INCREMENT,
    txt_template_code VARCHAR(64) NOT NULL COMMENT 'Stable code used by application e.g. USER_WELCOME',
    txt_template_name VARCHAR(200) NOT NULL,
    txt_description VARCHAR(500) NULL,
    txt_subject VARCHAR(500) NOT NULL COMMENT 'May contain {{placeholders}}',
    txt_body_html LONGTEXT NOT NULL COMMENT 'HTML fragment or full HTML; may contain {{placeholders}}',
    ser_email_layout_id INT NULL COMMENT 'When set, body is injected into layout {{content}}',
    txt_placeholder_help LONGTEXT NULL COMMENT 'JSON array documenting placeholders for admin UI',
    txt_category VARCHAR(64) NULL COMMENT 'e.g. USER, DELEGATION, WORKFLOW, SYSTEM',
    int_sort_order INT NOT NULL DEFAULT 100,
    bl_is_active TINYINT(1) NOT NULL DEFAULT 1,
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    ser_created_user_id INT NULL,
    ser_modified_user_id INT NULL,
    PRIMARY KEY (ser_email_template_id),
    UNIQUE KEY uk_email_template_code (txt_template_code),
    KEY idx_email_template_category (txt_category, bl_is_deleted, bl_is_active),
    KEY idx_email_template_layout (ser_email_layout_id),
    CONSTRAINT fk_email_template_layout
        FOREIGN KEY (ser_email_layout_id) REFERENCES cfg_tbl_email_layout (ser_email_layout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Planned txt_template_code values (application enforces; not a DB enum):
--   USER_WELCOME
--   USER_PASSWORD
--   USER_CHANGE_PASSWORD
--   DELEGATION_DELEGATE
--   DELEGATION_ON_BEHALF
--   TEMPLATE_APPROVAL_PENDING
--   TEMPLATE_APPROVED
--   TEMPLATE_REJECTED
--   TEMPLATE_SENDBACK
--   APPROVAL_REMINDER

SELECT 'cfg_tbl_email_layout and cfg_tbl_email_template ready' AS message;
