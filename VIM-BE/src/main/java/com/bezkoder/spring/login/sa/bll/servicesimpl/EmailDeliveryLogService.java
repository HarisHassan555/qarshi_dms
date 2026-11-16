package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.EmailDeliveryLogDto;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblEmailDeliveryLog;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblEmailDeliveryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailDeliveryLogService {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private static final int MAX_BODY_CHARS = 400_000;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    private CfgTblEmailDeliveryLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            List<String> recipients,
            String subject,
            String htmlBody,
            String textBody,
            boolean hasAttachments,
            String status,
            String errorMessage) {
        EmailSendContext ctx = EmailSendContext.get();
        List<String> normalizedRecipients = normalizeRecipients(recipients);
        if (normalizedRecipients.isEmpty()) {
            persistRow("(none)", subject, htmlBody, textBody, hasAttachments, status, errorMessage, ctx);
            return;
        }
        for (String email : normalizedRecipients) {
            persistRow(email, subject, htmlBody, textBody, hasAttachments, status, errorMessage, ctx);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String status, String triggerType, String recipient,
            Integer applicationId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<CfgTblEmailDeliveryLog> result = repository.search(
                emptyToNull(status),
                emptyToNull(triggerType),
                emptyToNull(recipient),
                applicationId,
                pageable);

        List<EmailDeliveryLogDto> items = new ArrayList<>();
        for (CfgTblEmailDeliveryLog row : result.getContent()) {
            items.add(toListDto(row));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("total", result.getTotalElements());
        response.put("page", safePage);
        response.put("size", safeSize);
        return response;
    }

    @Transactional(readOnly = true)
    public EmailDeliveryLogDto getById(Long id) {
        if (id == null) {
            return null;
        }
        return repository.findBySerEmailDeliveryLogIdAndBlIsDeletedFalse(id)
                .map(this::toDetailDto)
                .orElse(null);
    }

    private void persistRow(
            String recipientEmail,
            String subject,
            String htmlBody,
            String textBody,
            boolean hasAttachments,
            String status,
            String errorMessage,
            EmailSendContext ctx) {
        CfgTblEmailDeliveryLog row = new CfgTblEmailDeliveryLog();
        Timestamp now = Timestamp.from(Instant.now());
        row.setTxtRecipientEmail(trim(recipientEmail, 255));
        row.setTxtSubject(trim(subject, 512));
        row.setTxtBodyHtml(truncate(htmlBody));
        row.setTxtBodyText(truncate(textBody));
        row.setTxtStatus(status != null ? status : STATUS_SKIPPED);
        row.setTxtErrorMessage(truncate(errorMessage));
        row.setBlnHasAttachments(hasAttachments);
        row.setDteSentAt(now);
        row.setDteCreatedDate(now);
        row.setBlIsDeleted(false);
        row.setBlnStatus(true);

        if (ctx != null) {
            row.setTxtTriggerType(trim(ctx.getTriggerType(), 64));
            row.setTxtTemplateCode(trim(ctx.getTemplateCode(), 64));
            row.setSerApplicationId(ctx.getApplicationId());
            row.setSerTriggeredByUserId(ctx.getTriggeredByUserId());
            row.setSerBackgroundJobId(ctx.getBackgroundJobId());
            row.setTxtContextLabel(trim(ctx.getContextLabel(), 255));
        } else {
            row.setTxtTriggerType("SYSTEM");
        }

        repository.save(row);
    }

    private EmailDeliveryLogDto toListDto(CfgTblEmailDeliveryLog row) {
        EmailDeliveryLogDto dto = new EmailDeliveryLogDto();
        dto.setId(row.getSerEmailDeliveryLogId());
        dto.setTriggerType(row.getTxtTriggerType());
        dto.setTemplateCode(row.getTxtTemplateCode());
        dto.setApplicationId(row.getSerApplicationId());
        dto.setRecipientEmail(row.getTxtRecipientEmail());
        dto.setSubject(row.getTxtSubject());
        dto.setStatus(row.getTxtStatus());
        dto.setErrorMessage(row.getTxtErrorMessage());
        dto.setHasAttachments(row.isBlnHasAttachments());
        dto.setContextLabel(row.getTxtContextLabel());
        dto.setSentAt(formatTs(row.getDteSentAt()));
        return dto;
    }

    private EmailDeliveryLogDto toDetailDto(CfgTblEmailDeliveryLog row) {
        EmailDeliveryLogDto dto = toListDto(row);
        dto.setBodyHtml(row.getTxtBodyHtml());
        dto.setBodyText(row.getTxtBodyText());
        dto.setTriggeredByUserId(row.getSerTriggeredByUserId());
        dto.setBackgroundJobId(row.getSerBackgroundJobId());
        return dto;
    }

    private List<String> normalizeRecipients(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String raw : recipients) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (!trimmed.isEmpty() && !out.contains(trimmed)) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_BODY_CHARS) {
            return value;
        }
        return value.substring(0, MAX_BODY_CHARS) + "\n<!-- truncated -->";
    }

    private String trim(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private String formatTs(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ISO.format(ts.toInstant().atZone(ZoneId.systemDefault()));
    }
}