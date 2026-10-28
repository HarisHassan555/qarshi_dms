-- =====================================================
-- Red-branded layout + USER_WELCOME template (email module)
-- Run after create_email_template_tables.sql
-- Database: velocity
-- =====================================================

USE velocity;

INSERT INTO cfg_tbl_email_layout (
    txt_layout_code,
    txt_layout_name,
    txt_description,
    txt_html_wrapper,
    bl_is_active,
    bl_is_deleted,
    bln_status,
    dte_created_date,
    ser_created_user_id
) VALUES (
    'QARSHI_RED_WELCOME',
    'Qarshi Red – Welcome',
    'Red header/footer wrapper for welcome and user credential emails. Inserts template body at {{content}}.',
    '<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Qarshi DMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f3f4f6;font-family:Arial,Helvetica,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color:#f3f4f6;">
<tr>
<td align="center" style="padding:28px 16px;">
<table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;width:100%;background-color:#ffffff;border-radius:10px;overflow:hidden;border:1px solid #e5e7eb;box-shadow:0 4px 14px rgba(0,0,0,0.06);">
<tr>
<td style="background-color:#c8102e;padding:32px 28px;text-align:center;">
<p style="margin:0 0 6px;font-size:11px;letter-spacing:2px;text-transform:uppercase;color:#fecaca;">Welcome</p>
<h1 style="margin:0;font-size:24px;font-weight:bold;color:#ffffff;line-height:1.3;">Qarshi DMS</h1>
</td>
</tr>
<tr>
<td style="padding:32px 28px 28px;color:#1f2937;font-size:15px;line-height:1.65;">
{{content}}
</td>
</tr>
<tr>
<td style="background-color:#fef2f2;padding:18px 28px;border-top:4px solid #c8102e;">
<p style="margin:0;font-size:12px;color:#991b1b;text-align:center;line-height:1.5;">Automated message from <strong>Qarshi DMS</strong>. Keep your password confidential.</p>
</td>
</tr>
</table>
</td>
</tr>
</table>
</body>
</html>',
    1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_layout_name = VALUES(txt_layout_name),
    txt_description = VALUES(txt_description),
    txt_html_wrapper = VALUES(txt_html_wrapper),
    bl_is_active = 1,
    bl_is_deleted = 0,
    bln_status = 1,
    dte_modified_date = NOW();

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
    'USER_WELCOME',
    'User welcome – account created',
    'Sent when a new user is created (Users page / SAP). Uses red welcome layout.',
    'Welcome to Qarshi DMS – Your account details',
    '<h2 style="margin:0 0 16px;font-size:20px;color:#c8102e;font-weight:bold;">Welcome aboard</h2>
<p style="margin:0 0 12px;">Dear {{displayName}},</p>
<p style="margin:0 0 20px;">Your user account has been created. You can sign in using the credentials below.</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 22px;">
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Login name</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{loginName}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Password</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;font-family:Consolas,Monaco,monospace;">{{plainPassword}}</td>
</tr>
</table>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{loginUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">Sign in to DMS</a>
</p>
<p style="margin:0;font-size:12px;color:#6b7280;line-height:1.5;">Please change your password after your first login if prompted.<br>If you did not expect this email, contact your system administrator.</p>',
    @layout_id,
    '["displayName","loginName","plainPassword","loginUrl"]',
    'USER',
    10,
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
    'USER_PASSWORD',
    'User credentials – standard',
    'Default for new users from Users screen when welcome flag is off; same red layout as USER_WELCOME.',
    'Your Qarshi DMS account credentials',
    '<h2 style="margin:0 0 16px;font-size:20px;color:#c8102e;font-weight:bold;">Your account is ready</h2>
<p style="margin:0 0 12px;">Dear {{displayName}},</p>
<p style="margin:0 0 20px;">Use the credentials below to sign in to Qarshi DMS.</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 22px;">
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Login name</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{loginName}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Password</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;font-family:Consolas,Monaco,monospace;">{{plainPassword}}</td>
</tr>
</table>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{loginUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">Sign in to DMS</a>
</p>
<p style="margin:0;font-size:12px;color:#6b7280;line-height:1.5;">Please change your password after your first login if prompted.</p>',
    @layout_id,
    '["displayName","loginName","plainPassword","loginUrl"]',
    'USER',
    20,
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

SELECT
    @layout_id AS ser_email_layout_id,
    (SELECT ser_email_template_id FROM cfg_tbl_email_template WHERE txt_template_code = 'USER_WELCOME' LIMIT 1) AS ser_email_template_id,
    'USER_WELCOME + USER_PASSWORD + QARSHI_RED_WELCOME seeded' AS message;
