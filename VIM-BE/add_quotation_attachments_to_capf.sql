USE vim_3;

-- Insert Quotation Attachments to CAPF Form (ID 572)
INSERT INTO cfg_tbl_custom_form_field (
    ser_form_id,
    txt_field_label,
    txt_field_type,
    txt_placeholder,
    bl_is_required,
    int_field_order,
    bl_is_active,
    bl_is_deleted,
    dte_created_date,
    ser_created_user
) VALUES (
    572,
    'Quotation Attachments', 
    'multi_attachment',
    'Upload quotations',
    0, 
    14, 
    1, 
    0,
    NOW(),
    145
);

-- Verify the insertion
SELECT * FROM cfg_tbl_custom_form_field 
WHERE ser_form_id = 572 
ORDER BY int_field_order DESC 
LIMIT 5;
