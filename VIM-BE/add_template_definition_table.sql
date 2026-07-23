CREATE TABLE IF NOT EXISTS tpl_template_definition (
  ser_template_id INT AUTO_INCREMENT PRIMARY KEY,
  ser_form_id INT NOT NULL,
  txt_template_name VARCHAR(255),
  txt_code_convention VARCHAR(100),
  txt_template_payload LONGTEXT NOT NULL,
  bl_is_active TINYINT(1) DEFAULT 1,
  bl_is_deleted TINYINT(1) DEFAULT 0,
  bln_status TINYINT(1) DEFAULT 1,
  dte_created_date DATETIME,
  dte_modified_date DATETIME,
  ser_created_user INT,
  ser_modified_user INT,
  UNIQUE KEY uk_tpl_template_definition_form (ser_form_id)
);
  