-- Activity log table for login, approve, reject, sendback, update, form submission, etc.
CREATE TABLE IF NOT EXISTS cfg_tbl_app_activity_log (
    ser_activity_log_id INT NOT NULL AUTO_INCREMENT,
    txt_action_type VARCHAR(64) DEFAULT NULL,
    ser_user_id INT DEFAULT NULL,
    txt_username VARCHAR(255) DEFAULT NULL,
    txt_ip_address VARCHAR(64) DEFAULT NULL,
    txt_device VARCHAR(512) DEFAULT NULL,
    dte_created_date DATETIME DEFAULT NULL,
    txt_status VARCHAR(32) DEFAULT NULL,
    txt_message VARCHAR(2000) DEFAULT NULL,
    ser_entity_id INT DEFAULT NULL,
    txt_entity_type VARCHAR(64) DEFAULT NULL,
    txt_payload LONGTEXT DEFAULT NULL,
    txt_error_message VARCHAR(2000) DEFAULT NULL,
    PRIMARY KEY (ser_activity_log_id),
    KEY idx_activity_action_type (txt_action_type),
    KEY idx_activity_user_id (ser_user_id),
    KEY idx_activity_entity (txt_entity_type, ser_entity_id),
    KEY idx_activity_created (dte_created_date),
    KEY idx_activity_status (txt_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
