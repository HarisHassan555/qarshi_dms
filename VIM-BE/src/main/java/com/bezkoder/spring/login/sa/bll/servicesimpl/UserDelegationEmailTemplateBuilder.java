package com.bezkoder.spring.login.sa.bll.servicesimpl;

/**
 * HTML email layouts for user delegation notifications (Qarshi DMS branding).
 */
public final class UserDelegationEmailTemplateBuilder {

    public static final String APPLICATION_NAME = "Qarshi DMS";

    private UserDelegationEmailTemplateBuilder() {
    }

    public static String buildDelegateEmail(
            String recipientName,
            String onBehalfName,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes,
            String actionWord,
            String templateListUrl) {
        String headline = "Delegation " + capitalize(actionWord) + " – Delegate assignment";
        String intro = "A form delegation has been <strong>" + escapeHtml(actionWord) + "</strong> in "
                + escapeHtml(APPLICATION_NAME) + ".";
        String roleMessage = "You are registered as the <strong>delegate</strong>. You may create or submit the "
                + "digital documents listed below on behalf of <strong>" + escapeHtml(onBehalfName) + "</strong> "
                + "during the valid period.";
        String closing = "Please sign in to " + escapeHtml(APPLICATION_NAME)
                + " and open <strong>Digital Document List</strong> when you are ready to work on these forms.";
        return wrapEmail(
                headline,
                recipientName,
                intro,
                roleMessage,
                validFrom,
                validTo,
                status,
                documentsLabel,
                notes,
                closing,
                templateListUrl,
                "Open Digital Document List");
    }

    public static String buildOnBehalfEmail(
            String recipientName,
            String delegateName,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes,
            String actionWord,
            String templateListUrl) {
        String headline = "Delegation " + capitalize(actionWord) + " – On your behalf";
        String intro = "A form delegation has been <strong>" + escapeHtml(actionWord) + "</strong> in "
                + escapeHtml(APPLICATION_NAME) + ".";
        String roleMessage = "<strong>" + escapeHtml(delegateName) + "</strong> is authorized as "
                + "<strong>delegate</strong> to create or submit the documents below on your behalf. "
                + "Actions performed under this delegation will be attributed to you for the valid period.";
        String closing = "If you did not expect this delegation, please contact your system administrator immediately.";
        return wrapEmail(
                headline,
                recipientName,
                intro,
                roleMessage,
                validFrom,
                validTo,
                status,
                documentsLabel,
                notes,
                closing,
                templateListUrl,
                "View Digital Document List");
    }

    public static String subjectForDelegate(String actionWord, String onBehalfName) {
        return "[" + APPLICATION_NAME + "] Delegation " + actionWord + " – act on behalf of " + onBehalfName;
    }

    public static String subjectForOnBehalf(String actionWord, String delegateName) {
        return "[" + APPLICATION_NAME + "] Delegation " + actionWord + " – " + delegateName + " may act for you";
    }

    private static String wrapEmail(
            String headline,
            String recipientName,
            String introParagraph,
            String roleParagraph,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes,
            String closingParagraph,
            String templateListUrl,
            String buttonLabel) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>").append(escapeHtml(APPLICATION_NAME)).append("</title>");
        appendStyles(html);
        html.append("</head><body style='margin:0;padding:0;background-color:#f5f5f5;'>");
        html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='background-color:#f5f5f5;'>");
        html.append("<tr><td align='center' style='padding:20px 12px;'>");
        html.append("<table role='presentation' width='640' cellpadding='0' cellspacing='0' border='0' "
                + "style='max-width:640px;width:100%;background-color:#ffffff;border-radius:8px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.08);'>");

        html.append("<tr><td style='background-color:#1e3a5f;padding:22px 28px;border-radius:8px 8px 0 0;'>");
        html.append("<div style='font-size:13px;letter-spacing:0.04em;text-transform:uppercase;color:#b8c9e0;margin-bottom:6px;'>");
        html.append(escapeHtml(APPLICATION_NAME));
        html.append("</div>");
        html.append("<h1 style='margin:0;font-size:22px;font-weight:600;color:#ffffff;line-height:1.35;'>");
        html.append(escapeHtml(headline));
        html.append("</h1></td></tr>");

        html.append("<tr><td style='padding:28px 28px 8px 28px;font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;"
                + "font-size:15px;line-height:1.6;color:#333333;'>");
        html.append("<p style='margin:0 0 16px 0;color:#555555;'>Dear ").append(escapeHtml(recipientName)).append(",</p>");
        html.append("<p style='margin:0 0 14px 0;'>").append(introParagraph).append("</p>");
        html.append("<p style='margin:0 0 20px 0;'>").append(roleParagraph).append("</p>");
        appendDetailsBox(html, validFrom, validTo, status, documentsLabel, notes);
        html.append("<p style='margin:20px 0 0 0;color:#555555;'>").append(closingParagraph).append("</p>");
        appendButton(html, templateListUrl, buttonLabel);
        html.append("</td></tr>");

        html.append("<tr><td style='padding:16px 28px 24px 28px;border-top:1px solid #ecf0f1;"
                + "font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;font-size:12px;color:#95a5a6;text-align:center;'>");
        html.append("This is an automated message from <strong>").append(escapeHtml(APPLICATION_NAME)).append("</strong>. ");
        html.append("Please do not reply to this email.");
        html.append("</td></tr>");

        html.append("</table></td></tr></table></body></html>");
        return html.toString();
    }

    private static void appendDetailsBox(
            StringBuilder html,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes) {
        html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' "
                + "style='background-color:#f8f9fa;border-left:4px solid #3b82f6;border-radius:4px;margin:8px 0;'>");
        html.append("<tr><td style='padding:16px 18px;font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;font-size:14px;'>");
        html.append(detailRow("Valid from", escapeHtml(validFrom)));
        html.append(detailRow("Valid to", escapeHtml(validTo)));
        html.append(detailRow("Status", statusBadge(status)));
        html.append(detailRow("Documents", escapeHtml(documentsLabel)));
        if (notes != null && !notes.trim().isEmpty()) {
            html.append(detailRow("Notes", escapeHtml(notes.trim())));
        }
        html.append("</td></tr></table>");
    }

    private static String detailRow(String label, String valueHtml) {
        return "<div style='margin:8px 0;'>"
                + "<span style='display:inline-block;min-width:120px;font-weight:600;color:#555555;'>"
                + escapeHtml(label) + ":</span> "
                + "<span style='color:#333333;'>" + valueHtml + "</span></div>";
    }

    private static String statusBadge(String status) {
        String normalized = status != null ? status.trim().toUpperCase() : "ACTIVE";
        String bg = "#d6eaf8";
        String color = "#1d4ed8";
        if ("ACTIVE".equals(normalized)) {
            bg = "#d5f4e6";
            color = "#15803d";
        } else if ("SCHEDULED".equals(normalized)) {
            bg = "#e0e7ff";
            color = "#4338ca";
        } else if ("EXPIRED".equals(normalized)) {
            bg = "#f3f4f6";
            color = "#4b5563";
        } else if ("REVOKED".equals(normalized)) {
            bg = "#fadbd8";
            color = "#b91c1c";
        }
        return "<span style='display:inline-block;padding:3px 10px;border-radius:12px;font-size:11px;"
                + "font-weight:600;text-transform:uppercase;background-color:" + bg + ";color:" + color + ";'>"
                + escapeHtml(normalized) + "</span>";
    }

    private static void appendButton(StringBuilder html, String url, String label) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        html.append("<table role='presentation' width='100%' cellpadding='0' cellspacing='0' border='0' style='margin:24px 0 8px 0;'>");
        html.append("<tr><td align='center'>");
        html.append("<a href='").append(escapeHtml(url.trim())).append("' "
                + "style='display:inline-block;padding:12px 28px;background-color:#2563eb;color:#ffffff !important;"
                + "text-decoration:none;border-radius:6px;font-weight:600;font-size:15px;"
                + "font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;'>");
        html.append(escapeHtml(label));
        html.append("</a></td></tr></table>");
    }

    private static void appendStyles(StringBuilder html) {
        html.append("<style>");
        html.append("body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;line-height:1.6;color:#333;margin:0;padding:0;}");
        html.append("table{border-collapse:collapse;mso-table-lspace:0pt;mso-table-rspace:0pt;}");
        html.append("a{color:#2563eb;}");
        html.append("</style>");
        html.append("<!--[if mso]><style type='text/css'>table{border-collapse:collapse;}</style><![endif]-->");
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
