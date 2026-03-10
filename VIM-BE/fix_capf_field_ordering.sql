USE vim_3;

-- Explicitly force the exact order for all fields in the CAPF form (ID 572)
UPDATE cfg_tbl_custom_form_field SET int_field_order = 0 WHERE ser_field_id = 778; -- Division/Department
UPDATE cfg_tbl_custom_form_field SET int_field_order = 1 WHERE ser_field_id = 779; -- Date
UPDATE cfg_tbl_custom_form_field SET int_field_order = 2 WHERE ser_field_id = 780; -- Name of Asset/Item
UPDATE cfg_tbl_custom_form_field SET int_field_order = 3 WHERE ser_field_id = 781; -- Details & Specification
UPDATE cfg_tbl_custom_form_field SET int_field_order = 4 WHERE ser_field_id = 782; -- Utility & Purpose
UPDATE cfg_tbl_custom_form_field SET int_field_order = 5 WHERE ser_field_id = 783; -- FEASIBILITY REPORT ATTACHED
UPDATE cfg_tbl_custom_form_field SET int_field_order = 6 WHERE ser_field_id = 793; -- Quotation Attachments
UPDATE cfg_tbl_custom_form_field SET int_field_order = 7 WHERE ser_field_id = 784; -- IF NO THEN MENTION REASON:
UPDATE cfg_tbl_custom_form_field SET int_field_order = 8 WHERE ser_field_id = 785; -- Vendor Name
UPDATE cfg_tbl_custom_form_field SET int_field_order = 9 WHERE ser_field_id = 790; -- Address 
UPDATE cfg_tbl_custom_form_field SET int_field_order = 10 WHERE ser_field_id = 786; -- Approved price
UPDATE cfg_tbl_custom_form_field SET int_field_order = 11 WHERE ser_field_id = 787; -- delivery period
UPDATE cfg_tbl_custom_form_field SET int_field_order = 12 WHERE ser_field_id = 788; -- Terms & Conditions
UPDATE cfg_tbl_custom_form_field SET int_field_order = 13 WHERE ser_field_id = 789; -- Thrid party assessment carried out
UPDATE cfg_tbl_custom_form_field SET int_field_order = 14 WHERE ser_field_id = 791; -- Helping Document (If present)
