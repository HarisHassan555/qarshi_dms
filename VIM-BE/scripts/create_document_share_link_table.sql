-- View-only shareable document links (run against velocity database)
CREATE TABLE IF NOT EXISTS cfg_tbl_document_share_link (
    ser_document_share_id INT NOT NULL AUTO_INCREMENT,
    ser_application_id INT NOT NULL,
    txt_share_token VARCHAR(64) NOT NULL,
    ser_created_by_user_id INT NOT NULL,
    dte_created_date DATETIME NULL,
    dte_expires_date DATETIME NULL,
    bl_revoked TINYINT(1) NULL DEFAULT 0,
    dte_revoked_date DATETIME NULL,
    int_view_count INT NULL DEFAULT 0,
    dte_last_viewed_date DATETIME NULL,
    txt_scope VARCHAR(32) NOT NULL DEFAULT 'pdf_view',
    bl_is_deleted TINYINT(1) NULL DEFAULT 0,
    PRIMARY KEY (ser_document_share_id),
    UNIQUE KEY uk_document_share_token (txt_share_token),
    KEY idx_document_share_application (ser_application_id)
);
