package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.EmailLayoutDto;
import com.bezkoder.spring.login.sa.bll.dto.EmailLayoutSaveRequest;
import com.bezkoder.spring.login.sa.bll.dto.EmailTemplateDto;
import com.bezkoder.spring.login.sa.bll.dto.EmailTemplateSaveRequest;

import java.util.List;

public interface IEmailTemplateAdminService {

    List<EmailLayoutDto> getAllLayouts();

    EmailLayoutDto getLayoutById(int id);

    EmailLayoutDto createLayout(EmailLayoutSaveRequest request, int userId);

    EmailLayoutDto updateLayout(EmailLayoutSaveRequest request, int userId);

    String deleteLayoutsByIds(List<Integer> ids, int userId);

    List<EmailTemplateDto> getAllTemplates(String category);

    EmailTemplateDto getTemplateById(int id);

    EmailTemplateDto createTemplate(EmailTemplateSaveRequest request, int userId);

    EmailTemplateDto updateTemplate(EmailTemplateSaveRequest request, int userId);

    String deleteTemplatesByIds(List<Integer> ids, int userId);
}
