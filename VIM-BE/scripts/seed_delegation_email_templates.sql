-- =====================================================
-- Delegation notification emails (delegate + on-behalf) – red Qarshi layout
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
    'DELEGATION_DELEGATE',
    'Delegation – notify delegate',
    'Sent to the delegate when a delegation is created or updated.',
    '[{{applicationName}}] Delegation {{actionWordTitle}} – act on behalf of {{onBehalfName}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Delegation {{actionWordTitle}} – Delegate assignment</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 14px;">A form delegation has been <strong>{{actionWord}}</strong> in <strong>{{applicationName}}</strong>.</p>
<p style="margin:0 0 20px;">You are registered as the <strong>delegate</strong>. You may create or submit the digital documents listed below on behalf of <strong>{{onBehalfName}}</strong> during the valid period.</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 22px;">
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:32%;">Valid from</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{validFrom}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Valid to</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{validTo}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Status</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{status}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Documents</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{documentsLabel}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Notes</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{notes}}</td>
</tr>
</table>
<p style="margin:0 0 20px;color:#4b5563;">Please sign in to {{applicationName}} and open <strong>Digital Document List</strong> when you are ready to work on these forms.</p>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{templateListUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">Open Digital Document List</a>
</p>
<p style="margin:0;font-size:12px;color:#6b7280;">This is an automated message. Please do not reply to this email.</p>',
    @layout_id,
    '["recipientName","delegateName","onBehalfName","validFrom","validTo","status","documentsLabel","notes","actionWord","actionWordTitle","templateListUrl","applicationName"]',
    'DELEGATION',
    30,
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
    'DELEGATION_ON_BEHALF',
    'Delegation – notify on-behalf user',
    'Sent to the on-behalf user when a delegation is created or updated.',
    '[{{applicationName}}] Delegation {{actionWordTitle}} – {{delegateName}} may act for you',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Delegation {{actionWordTitle}} – On your behalf</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 14px;">A form delegation has been <strong>{{actionWord}}</strong> in <strong>{{applicationName}}</strong>.</p>
<p style="margin:0 0 20px;"><strong>{{delegateName}}</strong> is authorized as <strong>delegate</strong> to create or submit the documents below on your behalf. Actions performed under this delegation will be attributed to you for the valid period.</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 22px;">
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:32%;">Valid from</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{validFrom}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Valid to</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{validTo}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Status</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{status}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Documents</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{documentsLabel}}</td>
</tr>
<tr>
<td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Notes</td>
<td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{notes}}</td>
</tr>
</table>
<p style="margin:0 0 20px;color:#4b5563;">If you did not expect this delegation, please contact your system administrator immediately.</p>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{templateListUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">View Digital Document List</a>
</p>
<p style="margin:0;font-size:12px;color:#6b7280;">This is an automated message. Please do not reply to this email.</p>',
    @layout_id,
    '["recipientName","delegateName","onBehalfName","validFrom","validTo","status","documentsLabel","notes","actionWord","actionWordTitle","templateListUrl","applicationName"]',
    'DELEGATION',
    31,
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

SELECT 'DELEGATION_DELEGATE and DELEGATION_ON_BEHALF seeded' AS message,
       @layout_id AS layout_id;
