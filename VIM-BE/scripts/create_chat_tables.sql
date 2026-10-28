-- Internal user chat (1-to-1). Run against velocity_workbench (adjust USE as needed).

USE velocity_workbench;

CREATE TABLE IF NOT EXISTS chat_tbl_conversation (
    ser_conversation_id INT NOT NULL AUTO_INCREMENT,
    txt_type VARCHAR(16) NOT NULL DEFAULT 'DIRECT',
    dte_created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ser_conversation_id),
    KEY idx_chat_conv_type (txt_type)
);

CREATE TABLE IF NOT EXISTS chat_tbl_conversation_member (
    ser_member_id INT NOT NULL AUTO_INCREMENT,
    ser_conversation_id INT NOT NULL,
    ser_user_id INT NOT NULL,
    ser_last_read_message_id INT NULL,
    dte_joined_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ser_member_id),
    UNIQUE KEY uk_chat_member_conv_user (ser_conversation_id, ser_user_id),
    KEY idx_chat_member_user (ser_user_id),
    CONSTRAINT fk_chat_member_conv FOREIGN KEY (ser_conversation_id) REFERENCES chat_tbl_conversation (ser_conversation_id)
);

CREATE TABLE IF NOT EXISTS chat_tbl_message (
    ser_message_id INT NOT NULL AUTO_INCREMENT,
    ser_conversation_id INT NOT NULL,
    ser_sender_user_id INT NOT NULL,
    txt_body VARCHAR(4000) NOT NULL,
    dte_created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ser_message_id),
    KEY idx_chat_msg_conv_date (ser_conversation_id, dte_created_date),
    CONSTRAINT fk_chat_msg_conv FOREIGN KEY (ser_conversation_id) REFERENCES chat_tbl_conversation (ser_conversation_id)
);
