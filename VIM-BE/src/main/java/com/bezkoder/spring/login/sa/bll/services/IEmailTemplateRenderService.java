package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.RenderedEmailMessage;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface IEmailTemplateRenderService {

    Optional<RenderedEmailMessage> renderActiveTemplate(String templateCode, Map<String, String> placeholders);

    Optional<RenderedEmailMessage> renderActiveTemplate(
            String templateCode, Map<String, String> placeholders, Set<String> rawHtmlPlaceholderKeys);
}
