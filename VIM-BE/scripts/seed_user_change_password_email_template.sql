-- =====================================================
-- Change-password notification (admin reset + self-service)
-- Run after create_email_template_tables.sql and seed_user_welcome_email_red_layout.sql
-- Database: velocity
-- =====================================================

USE velocity;

SET @layout_id = (
    SELECT ser_email_layout_id
    FROM cfg_tbl_email_layout
    WHERE txt_layout_code = 'QARSHI_RED_WELCOME'
      AND bl_is_deleted = 0
    LIMIT 1
);

INSERT INTO cfg_tbl_email_template (
    txt_template_code,
    txt_template_name,
    txt_description,
    txt_subject,
    txt_body_html,
    ser_email_layout_id,
    txt_placeholder_help,
    txt_category,
    int_sort_order,
    bl_is_active,
    bl_is_deleted,
    bln_status,
    dte_created_date,
    ser_created_user_id
) VALUES (
    'USER_CHANGE_PASSWORD',
    'User – password changed',
    'Sent when a user password is reset (Change Password page: admin or self-service).',
    '[{{applicationName}}] Your password has been updated',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Password updated</h2>
<p style="margin:0 0 12px;">Dear {{displayName}},</p>
<p style="margin:0 0 20px;">{{changeMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 22px;">
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Login name</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{loginName}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">New password</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;font-family:Consolas,Monaco,monospace;">{{plainPassword}}</td>
</tr>
</table>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{loginUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">Sign in to DMS</a>
</p>
<p style="margin:0;font-size:12px;color:#6b7280;line-height:1.5;">If you did not request this change, contact your system administrator immediately.</p>',
    @layout_id,
    '["displayName","loginName","plainPassword","loginUrl","changeMessage","applicationName"]',
    'USER',
    25,
    1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_template_name = VALUES(txt_template_name),
    txt_description = VALUES(txt_description),
    txt_subject = VALUES(txt_subject),
    txt_body_html = VALUES(txt_body_html),
    ser_email_layout_id = @layout_id,
    txt_placeholder_help = VALUES(txt_placeholder_help),
    txt_category = VALUES(txt_category),
    int_sort_order = VALUES(int_sort_order),
    bl_is_active = 1,
    bl_is_deleted = 0,
    bln_status = 1,
    dte_modified_date = NOW();

SELECT 'USER_CHANGE_PASSWORD seeded' AS message, @layout_id AS layout_id;
