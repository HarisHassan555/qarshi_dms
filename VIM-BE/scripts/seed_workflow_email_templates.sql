-- =====================================================
-- Digital document / workflow emails (initiator + approver)
-- Run after:
--   create_email_template_tables.sql
--   seed_user_welcome_email_red_layout.sql  (for QARSHI_RED_WELCOME layout)
-- Note: Application code must be wired to use these codes (not done yet).
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

-- Shared detail block placeholders:
-- recipientName, introMessage, applicationCode, formName, approvalLevel, statusLabel, remarks,
-- actionButtonsHtml, approvalHistoryHtml, applicationName, myApplicationUrl

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
    'TEMPLATE_APPROVAL_PENDING',
    'Workflow – pending approval (approver)',
    'Sent to the next approver when an application needs action. Inject actionButtonsHtml from the app.',
    '[{{applicationName}}] {{formName}} {{approverSubjectSuffix}} - {{applicationCode}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">{{formName}}</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 16px;">{{introMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 18px;">
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Application code</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{applicationCode}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Form</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{formName}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Approval level</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{approvalLevel}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Status</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{statusLabel}}</td></tr>
</table>
<p style="margin:0 0 8px;">{{remarksBlock}}</p>
<div style="margin:24px 0;text-align:center;">{{actionButtonsHtml}}</div>
<p style="text-align:center;color:#6b7280;font-size:12px;margin:16px 0 0;">You can also open <a href="{{myApplicationUrl}}" style="color:#c8102e;">My Digital Applications</a> in {{applicationName}}.</p>
{{approvalHistoryHtml}}
<p style="margin:16px 0 0;font-size:12px;color:#6b7280;">This is an automated message. Please do not reply.</p>',
    @layout_id,
    '["recipientName","introMessage","applicationCode","formName","approvalLevel","statusLabel","remarksBlock","actionButtonsHtml","approvalHistoryHtml","applicationName","myApplicationUrl","approverSubjectSuffix"]',
    'WORKFLOW',
    100,
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
    txt_template_code, txt_template_name, txt_description, txt_subject, txt_body_html,
    ser_email_layout_id, txt_placeholder_help, txt_category, int_sort_order,
    bl_is_active, bl_is_deleted, bln_status, dte_created_date, ser_created_user_id
) VALUES (
    'TEMPLATE_APPROVED',
    'Workflow – progress update (initiator)',
    'Sent to the submitter when the application moves forward or is approved at a level.',
    '[{{applicationName}}] {{formName}} {{statusSubjectSuffix}} - {{applicationCode}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">{{formName}}</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 16px;">{{introMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 18px;">
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Application code</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{applicationCode}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Approval level</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{approvalLevel}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Status</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{statusLabel}}</td></tr>
</table>
<p style="margin:0 0 8px;">{{remarksBlock}}</p>
<p style="margin:0 0 16px;">Thank you for using {{applicationName}}.</p>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{myApplicationUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">View My Application</a>
</p>
{{approvalHistoryHtml}}',
    @layout_id,
    '["recipientName","introMessage","statusSubjectSuffix","applicationCode","formName","approvalLevel","statusLabel","remarksBlock","approvalHistoryHtml","applicationName","myApplicationUrl"]',
    'WORKFLOW', 110, 1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_template_name = VALUES(txt_template_name), txt_description = VALUES(txt_description),
    txt_subject = VALUES(txt_subject), txt_body_html = VALUES(txt_body_html),
    ser_email_layout_id = @layout_id, txt_placeholder_help = VALUES(txt_placeholder_help),
    txt_category = VALUES(txt_category), int_sort_order = VALUES(int_sort_order),
    bl_is_active = 1, bl_is_deleted = 0, bln_status = 1, dte_modified_date = NOW();

INSERT INTO cfg_tbl_email_template (
    txt_template_code, txt_template_name, txt_description, txt_subject, txt_body_html,
    ser_email_layout_id, txt_placeholder_help, txt_category, int_sort_order,
    bl_is_active, bl_is_deleted, bln_status, dte_created_date, ser_created_user_id
) VALUES (
    'TEMPLATE_REJECTED',
    'Workflow – rejected (initiator)',
    'Sent to the submitter when the application is rejected.',
    '[{{applicationName}}] {{formName}} Rejected - {{applicationCode}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Application rejected</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 16px;">{{introMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 18px;">
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Application code</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{applicationCode}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Form</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{formName}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Level</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{approvalLevel}}</td></tr>
</table>
<p style="margin:0 0 8px;">{{remarksBlock}}</p>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{myApplicationUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">View application</a>
</p>
{{approvalHistoryHtml}}',
    @layout_id,
    '["recipientName","introMessage","applicationCode","formName","approvalLevel","remarksBlock","approvalHistoryHtml","applicationName","myApplicationUrl"]',
    'WORKFLOW', 120, 1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_template_name = VALUES(txt_template_name), txt_description = VALUES(txt_description),
    txt_subject = VALUES(txt_subject), txt_body_html = VALUES(txt_body_html),
    ser_email_layout_id = @layout_id, txt_placeholder_help = VALUES(txt_placeholder_help),
    txt_category = VALUES(txt_category), int_sort_order = VALUES(int_sort_order),
    bl_is_active = 1, bl_is_deleted = 0, bln_status = 1, dte_modified_date = NOW();

INSERT INTO cfg_tbl_email_template (
    txt_template_code, txt_template_name, txt_description, txt_subject, txt_body_html,
    ser_email_layout_id, txt_placeholder_help, txt_category, int_sort_order,
    bl_is_active, bl_is_deleted, bln_status, dte_created_date, ser_created_user_id
) VALUES (
    'TEMPLATE_SENDBACK',
    'Workflow – sent back (initiator or approver)',
    'Sent back for revision; approver variant may include actionButtonsHtml.',
    '[{{applicationName}}] {{formName}} Sent Back - {{applicationCode}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Sent back for revision</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 16px;">{{introMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 18px;">
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Application code</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{applicationCode}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Level</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{approvalLevel}}</td></tr>
</table>
<p style="margin:0 0 8px;">{{remarksBlock}}</p>
<div style="margin:24px 0;text-align:center;">{{actionButtonsHtml}}</div>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{myApplicationUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">Open application</a>
</p>
{{approvalHistoryHtml}}',
    @layout_id,
    '["recipientName","introMessage","applicationCode","formName","approvalLevel","remarksBlock","actionButtonsHtml","approvalHistoryHtml","applicationName","myApplicationUrl"]',
    'WORKFLOW', 130, 1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_template_name = VALUES(txt_template_name), txt_description = VALUES(txt_description),
    txt_subject = VALUES(txt_subject), txt_body_html = VALUES(txt_body_html),
    ser_email_layout_id = @layout_id, txt_placeholder_help = VALUES(txt_placeholder_help),
    txt_category = VALUES(txt_category), int_sort_order = VALUES(int_sort_order),
    bl_is_active = 1, bl_is_deleted = 0, bln_status = 1, dte_modified_date = NOW();

INSERT INTO cfg_tbl_email_template (
    txt_template_code, txt_template_name, txt_description, txt_subject, txt_body_html,
    ser_email_layout_id, txt_placeholder_help, txt_category, int_sort_order,
    bl_is_active, bl_is_deleted, bln_status, dte_created_date, ser_created_user_id
) VALUES (
    'WORKFLOW_INITIATOR_COMPLETED',
    'Workflow – completed (initiator)',
    'Final notification when the application is fully approved/completed.',
    '[{{applicationName}}] {{formName}} Completed - {{applicationCode}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Application completed</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 16px;">{{introMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 18px;">
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Application code</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{applicationCode}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Status</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{statusLabel}}</td></tr>
</table>
<p style="margin:0 0 22px;text-align:center;">
<a href="{{myApplicationUrl}}" style="display:inline-block;padding:13px 32px;background-color:#c8102e;color:#ffffff;text-decoration:none;font-weight:bold;font-size:15px;border-radius:6px;">View completed application</a>
</p>
{{approvalHistoryHtml}}
<p style="margin:16px 0 0;font-size:12px;color:#6b7280;">Thank you for using {{applicationName}}.</p>',
    @layout_id,
    '["recipientName","introMessage","applicationCode","formName","statusLabel","approvalHistoryHtml","applicationName","myApplicationUrl"]',
    'WORKFLOW', 140, 1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_template_name = VALUES(txt_template_name), txt_description = VALUES(txt_description),
    txt_subject = VALUES(txt_subject), txt_body_html = VALUES(txt_body_html),
    ser_email_layout_id = @layout_id, txt_placeholder_help = VALUES(txt_placeholder_help),
    txt_category = VALUES(txt_category), int_sort_order = VALUES(int_sort_order),
    bl_is_active = 1, bl_is_deleted = 0, bln_status = 1, dte_modified_date = NOW();

INSERT INTO cfg_tbl_email_template (
    txt_template_code, txt_template_name, txt_description, txt_subject, txt_body_html,
    ser_email_layout_id, txt_placeholder_help, txt_category, int_sort_order,
    bl_is_active, bl_is_deleted, bln_status, dte_created_date, ser_created_user_id
) VALUES (
    'APPROVAL_REMINDER',
    'Workflow – approval reminder',
    'Reminder to approver (or current actor) to act on a pending application.',
    '[{{applicationName}}] Reminder: {{formName}} pending - {{applicationCode}}',
    '<h2 style="margin:0 0 12px;font-size:20px;color:#c8102e;font-weight:bold;">Approval reminder</h2>
<p style="margin:0 0 12px;">Dear {{recipientName}},</p>
<p style="margin:0 0 16px;">{{introMessage}}</p>
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="border-collapse:collapse;margin:0 0 18px;">
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;width:36%;">Application code</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{applicationCode}}</td></tr>
<tr><td style="padding:11px 14px;background-color:#fef2f2;border-left:4px solid #c8102e;font-weight:bold;color:#991b1b;">Level</td><td style="padding:11px 14px;background-color:#f9fafb;color:#111827;">{{approvalLevel}}</td></tr>
</table>
<div style="margin:24px 0;text-align:center;">{{actionButtonsHtml}}</div>
{{approvalHistoryHtml}}',
    @layout_id,
    '["recipientName","introMessage","applicationCode","formName","approvalLevel","actionButtonsHtml","approvalHistoryHtml","applicationName"]',
    'WORKFLOW', 150, 1, 0, 1, NOW(), 1
)
ON DUPLICATE KEY UPDATE
    txt_template_name = VALUES(txt_template_name), txt_description = VALUES(txt_description),
    txt_subject = VALUES(txt_subject), txt_body_html = VALUES(txt_body_html),
    ser_email_layout_id = @layout_id, txt_placeholder_help = VALUES(txt_placeholder_help),
    txt_category = VALUES(txt_category), int_sort_order = VALUES(int_sort_order),
    bl_is_active = 1, bl_is_deleted = 0, bln_status = 1, dte_modified_date = NOW();

SELECT txt_template_code, txt_template_name, txt_category, bl_is_active, ser_email_layout_id
FROM cfg_tbl_email_template
WHERE txt_category = 'WORKFLOW' AND bl_is_deleted = 0
ORDER BY int_sort_order;
