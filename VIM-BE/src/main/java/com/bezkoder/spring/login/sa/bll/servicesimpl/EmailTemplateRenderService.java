package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.RenderedEmailMessage;
import com.bezkoder.spring.login.sa.bll.services.IEmailTemplateRenderService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblEmailLayout;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblEmailTemplate;
import com.bezkoder.spring.login.sa.dal.entities.EmailLayoutRepository;
import com.bezkoder.spring.login.sa.dal.entities.EmailTemplateRepository;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailTemplateRenderService implements IEmailTemplateRenderService {

    private static final Logger logger = LogManager.getLogger(EmailTemplateRenderService.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    @Autowired
    private EmailTemplateRepository templateRepository;

    @Autowired
    private EmailLayoutRepository layoutRepository;

    @Override
    public Optional<RenderedEmailMessage> renderActiveTemplate(String templateCode, Map<String, String> placeholders) {
        return renderActiveTemplate(templateCode, placeholders, emptySkipSet());
    }

    @Override
    public Optional<RenderedEmailMessage> renderActiveTemplate(
            String templateCode, Map<String, String> placeholders, Set<String> rawHtmlPlaceholderKeys) {
        if (!StringUtils.hasText(templateCode)) {
            return Optional.empty();
        }
        Optional<CfgTblEmailTemplate> templateOpt =
                templateRepository.findByTxtTemplateCodeIgnoreCaseAndBlIsDeletedFalse(templateCode.trim());
        if (!templateOpt.isPresent()) {
            logger.info("Email template not found: " + templateCode);
            return Optional.empty();
        }
        CfgTblEmailTemplate template = templateOpt.get();
        if (!template.isBlIsActive()) {
            logger.info("Email template inactive: " + templateCode);
            return Optional.empty();
        }

        Set<String> rawKeys = normalizeRawHtmlKeys(rawHtmlPlaceholderKeys);
        Map<String, String> safe = escapePlaceholderValues(placeholders, rawKeys);
        String body = applyPlaceholders(template.getTxtBodyHtml(), safe, rawKeys);
        String html = body;

        Integer layoutId = template.getSerEmailLayoutId();
        if (layoutId != null && layoutId > 0) {
            Optional<CfgTblEmailLayout> layoutOpt = layoutRepository.findBySerEmailLayoutIdAndBlIsDeletedFalse(layoutId);
            if (layoutOpt.isPresent() && layoutOpt.get().isBlIsActive()) {
                String wrapper = layoutOpt.get().getTxtHtmlWrapper();
                html = mergeLayoutBody(wrapper, body);
                Set<String> skip = new HashSet<>();
                skip.add("content");
                skip.add("body");
                html = applyPlaceholders(html, safe, mergeSkipKeys(skip, rawKeys));
            }
        } else if (!looksLikeFullHtmlDocument(html)) {
            html = wrapFragmentAsDocument(html);
        }

        String subject = applyPlaceholders(template.getTxtSubject(), safe, rawKeys);
        return Optional.of(new RenderedEmailMessage(subject, html));
    }

    public Map<String, String> buildDelegationPlaceholders(
            String recipientName,
            String delegateName,
            String onBehalfName,
            String validFrom,
            String validTo,
            String status,
            String documentsLabel,
            String notes,
            String actionWord,
            String templateListUrl) {
        Map<String, String> map = new HashMap<>();
        map.put("recipientName", nullToDash(recipientName));
        map.put("delegateName", nullToDash(delegateName));
        map.put("onBehalfName", nullToDash(onBehalfName));
        map.put("validFrom", nullToDash(validFrom));
        map.put("validTo", nullToDash(validTo));
        map.put("status", nullToDash(status));
        map.put("documentsLabel", nullToDash(documentsLabel));
        map.put("notes", notes != null && !notes.trim().isEmpty() ? notes.trim() : "—");
        map.put("actionWord", actionWord != null ? actionWord.trim().toLowerCase() : "created");
        map.put("actionWordTitle", capitalizeWord(map.get("actionWord")));
        map.put("templateListUrl", templateListUrl != null ? templateListUrl.trim() : "");
        map.put("applicationName", "Qarshi DMS");
        return map;
    }

    public Map<String, String> buildUserCredentialPlaceholders(
            String displayName, String loginName, String plainPassword, String recipientEmail) {
        Map<String, String> map = new HashMap<>();
        String safeLogin = StringUtils.hasText(loginName) ? loginName.trim() : recipientEmail;
        String safeName = StringUtils.hasText(displayName) ? displayName.trim() : safeLogin;
        map.put("displayName", safeName);
        map.put("loginName", safeLogin);
        map.put("userName", safeLogin);
        map.put("plainPassword", plainPassword != null ? plainPassword : "");
        map.put("recipientEmail", recipientEmail != null ? recipientEmail.trim() : "");
        map.put("loginUrl", resolveLoginUrl());
        map.put("applicationName", "Qarshi DMS");
        return map;
    }

    public Map<String, String> buildChangePasswordPlaceholders(
            String displayName,
            String loginName,
            String plainPassword,
            String recipientEmail,
            boolean adminInitiated) {
        Map<String, String> map = buildUserCredentialPlaceholders(
                displayName, loginName, plainPassword, recipientEmail);
        if (adminInitiated) {
            map.put("changeMessage",
                    "An administrator has reset your " + map.get("applicationName")
                            + " password. Use the new password below to sign in.");
        } else {
            map.put("changeMessage",
                    "Your " + map.get("applicationName") + " password was changed successfully. "
                            + "If you made this change, you can sign in with your new password below.");
        }
        return map;
    }

    private String resolveLoginUrl() {
        String base = loadAppProperty("app.base.url", "http://localhost:4200/velocity");
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + "auth/login";
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

    private String mergeLayoutBody(String wrapper, String body) {
        if (wrapper == null) {
            return body;
        }
        if (wrapper.matches("(?s).*\\{\\{\\s*content\\s*\\}\\}.*")) {
            return wrapper.replaceAll("(?i)\\{\\{\\s*content\\s*\\}\\}", Matcher.quoteReplacement(body));
        }
        if (wrapper.matches("(?s).*\\{\\{\\s*body\\s*\\}\\}.*")) {
            return wrapper.replaceAll("(?i)\\{\\{\\s*body\\s*\\}\\}", Matcher.quoteReplacement(body));
        }
        if (wrapper.contains("[body]")) {
            return wrapper.replace("[body]", body);
        }
        return wrapper + body;
    }

    private String applyPlaceholders(String text, Map<String, String> values, Set<String> skipKeys) {
        if (text == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (skipKeys != null && skipKeys.contains(key.toLowerCase())) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String replacement = values.get(key);
            if (replacement == null) {
                replacement = values.get(key.toLowerCase());
            }
            if (replacement == null) {
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Map<String, String> escapePlaceholderValues(Map<String, String> placeholders, Set<String> rawHtmlKeys) {
        Map<String, String> safe = new HashMap<>();
        if (placeholders == null) {
            return safe;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (rawHtmlKeys != null && rawHtmlKeys.contains(key)) {
                safe.put(key, entry.getValue() != null ? entry.getValue() : "");
            } else if (rawHtmlKeys != null && rawHtmlKeys.contains(key.toLowerCase(Locale.ROOT))) {
                safe.put(key, entry.getValue() != null ? entry.getValue() : "");
            } else {
                safe.put(key, escapeHtml(entry.getValue()));
            }
        }
        return safe;
    }

    private Set<String> normalizeRawHtmlKeys(Set<String> rawHtmlPlaceholderKeys) {
        Set<String> normalized = new HashSet<>();
        if (rawHtmlPlaceholderKeys == null || rawHtmlPlaceholderKeys.isEmpty()) {
            return normalized;
        }
        for (String key : rawHtmlPlaceholderKeys) {
            if (key != null && !key.trim().isEmpty()) {
                normalized.add(key.trim());
            }
        }
        return normalized;
    }

    private Set<String> mergeSkipKeys(Set<String> layoutSkip, Set<String> rawKeys) {
        Set<String> merged = new HashSet<>();
        if (layoutSkip != null) {
            merged.addAll(layoutSkip);
        }
        if (rawKeys != null) {
            merged.addAll(rawKeys);
        }
        return merged;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static boolean looksLikeFullHtmlDocument(String html) {
        if (html == null) {
            return false;
        }
        String trimmed = html.trim().toLowerCase();
        return trimmed.startsWith("<!doctype") || trimmed.startsWith("<html");
    }

    private static String wrapFragmentAsDocument(String fragment) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body style=\"margin:0;padding:16px;font-family:Arial,sans-serif;\">"
                + fragment + "</body></html>";
    }

    private static Set<String> emptySkipSet() {
        return new HashSet<>();
    }

    private static String nullToDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "—";
        }
        return value.trim();
    }

    private static String capitalizeWord(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() == 1) {
            return value.toUpperCase();
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
