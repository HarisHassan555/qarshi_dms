-- =====================================================
-- In-app document workflow notifications (per user inbox)
-- Real-time delivery is application-layer (WebSocket); rows are the source of truth.
-- Database: velocity (change USE if your schema differs)
-- =====================================================

USE velocity;

CREATE TABLE IF NOT EXISTS cfg_tbl_user_notification (
    ser_user_notification_id BIGINT NOT NULL AUTO_INCREMENT,
    ser_recipient_user_id INT NOT NULL COMMENT 'cfg_tbl_user.ser_user_id',
    ser_application_id INT NULL COMMENT 'cfg_tbl_custom_form_application.ser_application_id',
    ser_form_id INT NULL COMMENT 'Template / form id for display',
    txt_event_type VARCHAR(64) NOT NULL COMMENT 'SUBMITTED, APPROVED, REJECTED, SENT_BACK, HOLD, OPINION_REQUESTED, COMPLETED, etc.',
    txt_title VARCHAR(255) NOT NULL,
    txt_message VARCHAR(2000) NULL,
    txt_deep_link VARCHAR(512) NULL COMMENT 'Angular route segment e.g. template-approval/410',
    txt_payload LONGTEXT NULL COMMENT 'Optional JSON: actorName, level, remarks, formCode',
    bln_read TINYINT(1) NOT NULL DEFAULT 0,
    dte_read_date DATETIME NULL,
    ser_actor_user_id INT NULL COMMENT 'User who triggered the event',
    bl_is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    bln_status TINYINT(1) NOT NULL DEFAULT 1,
    dte_created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dte_modified_date DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ser_user_notification_id),
    KEY idx_user_notification_recipient_unread (ser_recipient_user_id, bln_read, dte_created_date),
    KEY idx_user_notification_recipient_created (ser_recipient_user_id, dte_created_date),
    KEY idx_user_notification_application (ser_application_id),
    KEY idx_user_notification_event (txt_event_type, dte_created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT 'cfg_tbl_user_notification ready' AS message;
