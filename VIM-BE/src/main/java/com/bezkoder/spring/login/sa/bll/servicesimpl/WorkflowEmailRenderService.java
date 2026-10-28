package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.RenderedEmailMessage;
import com.bezkoder.spring.login.sa.bll.dto.WorkflowEmailRenderRequest;
import com.bezkoder.spring.login.sa.bll.services.IEmailTemplateRenderService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@Service
public class WorkflowEmailRenderService {

    private static final Logger logger = LogManager.getLogger(WorkflowEmailRenderService.class);

    @Autowired
    private IEmailTemplateRenderService emailTemplateRenderService;

    @Autowired
    private WorkflowEmailContentBuilder workflowEmailContentBuilder;

    public Optional<String> tryRenderWorkflowEmail(WorkflowEmailRenderRequest request, String approvalHistoryHtml) {
        if (request == null) {
            return Optional.empty();
        }
        WorkflowEmailRenderRequest enriched = enrichRequest(request);
        String templateCode = workflowEmailContentBuilder.resolveTemplateCode(enriched);
        Map<String, String> placeholders = workflowEmailContentBuilder.buildPlaceholders(enriched, approvalHistoryHtml);
        Optional<RenderedEmailMessage> rendered = emailTemplateRenderService.renderActiveTemplate(
                templateCode,
                placeholders,
                WorkflowEmailContentBuilder.RAW_HTML_PLACEHOLDER_KEYS);
        if (rendered.isPresent()) {
            logger.debug("Rendered workflow email using template " + templateCode);
            return Optional.of(rendered.get().getHtmlBody());
        }
        logger.debug("Workflow email template not used for code " + templateCode + "; falling back to legacy HTML");
        return Optional.empty();
    }

    public WorkflowEmailRenderRequest enrichRequest(WorkflowEmailRenderRequest request) {
        String spaBase = resolveSpaBaseUrl();
        String myAppUrl = spaBase + "/my-application";
        return WorkflowEmailRenderRequest.builder()
                .recipientName(request.getRecipientName())
                .level(request.getLevel())
                .applicationCode(request.getApplicationCode())
                .formName(request.getFormName())
                .status(request.getStatus())
                .remarks(request.getRemarks())
                .showActionButtons(request.isShowActionButtons())
                .approveUrl(request.getApproveUrl())
                .rejectUrl(request.getRejectUrl())
                .sendBackUrl(request.getSendBackUrl())
                .sendBackToInitiatorUrl(request.getSendBackToInitiatorUrl())
                .holdUrl(request.getHoldUrl())
                .unholdUrl(request.getUnholdUrl())
                .opinionUrl(request.getOpinionUrl())
                .finalCompletion(request.isFinalCompletion())
                .approvalReminder(request.isApprovalReminder())
                .statusSubjectSuffix(request.getStatusSubjectSuffix())
                .approverSubjectSuffix(request.getApproverSubjectSuffix())
                .myApplicationUrl(StringUtils.hasText(request.getMyApplicationUrl())
                        ? request.getMyApplicationUrl() : myAppUrl)
                .applicationName(StringUtils.hasText(request.getApplicationName())
                        ? request.getApplicationName() : "Qarshi DMS")
                .build();
    }

    private String resolveSpaBaseUrl() {
        String raw = loadAppProperty("app.base.url", "http://localhost:4200/velocity");
        raw = raw.replaceAll("/+$", "");
        if (raw.endsWith("/velocity")) {
            return raw;
        }
        return raw + "/velocity";
    }

    private String loadAppProperty(String key, String defaultValue) {
        try {
            Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));
            String value = props.getProperty(key);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        } catch (Exception ex) {
            logger.warn("Could not load property " + key + ": " + ex.getMessage());
        }
        return defaultValue;
    }
}
