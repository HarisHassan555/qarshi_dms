package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.dto.EmailLayoutDto;
import com.bezkoder.spring.login.sa.bll.dto.EmailLayoutSaveRequest;
import com.bezkoder.spring.login.sa.bll.dto.EmailTemplateDto;
import com.bezkoder.spring.login.sa.bll.dto.EmailTemplateSaveRequest;
import com.bezkoder.spring.login.sa.bll.services.IEmailTemplateAdminService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblEmailLayout;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblEmailTemplate;
import com.bezkoder.spring.login.sa.dal.entities.EmailLayoutRepository;
import com.bezkoder.spring.login.sa.dal.entities.EmailTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class EmailTemplateAdminService implements IEmailTemplateAdminService {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    @Autowired
    private EmailLayoutRepository layoutRepository;

    @Autowired
    private EmailTemplateRepository templateRepository;

    @Override
    public List<EmailLayoutDto> getAllLayouts() {
        List<EmailLayoutDto> result = new ArrayList<>();
        for (CfgTblEmailLayout row : layoutRepository.findByBlIsDeletedFalseOrderByTxtLayoutNameAsc()) {
            result.add(toLayoutDto(row));
        }
        return result;
    }

    @Override
    public EmailLayoutDto getLayoutById(int id) {
        return toLayoutDto(findLayoutOrThrow(id));
    }

    @Override
    @Transactional
    public EmailLayoutDto createLayout(EmailLayoutSaveRequest request, int userId) {
        validateLayoutSave(request, null);
        CfgTblEmailLayout row = new CfgTblEmailLayout();
        applyLayoutSave(row, request, userId, true);
        row.setSerCreatedUserId(userId > 0 ? userId : null);
        row.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        layoutRepository.save(row);
        return toLayoutDto(row);
    }

    @Override
    @Transactional
    public EmailLayoutDto updateLayout(EmailLayoutSaveRequest request, int userId) {
        if (request.getId() == null || request.getId() <= 0) {
            throw new IllegalArgumentException("id is required for update");
        }
        CfgTblEmailLayout row = findLayoutOrThrow(request.getId());
        validateLayoutSave(request, row.getSerEmailLayoutId());
        applyLayoutSave(row, request, userId, false);
        layoutRepository.save(row);
        return toLayoutDto(row);
    }

    @Override
    @Transactional
    public String deleteLayoutsByIds(List<Integer> ids, int userId) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No layout id(s) provided");
        }
        int deleted = 0;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Integer id : ids) {
            if (id == null || id <= 0) {
                continue;
            }
            Optional<CfgTblEmailLayout> optional = layoutRepository.findBySerEmailLayoutIdAndBlIsDeletedFalse(id);
            if (!optional.isPresent()) {
                continue;
            }
            long inUse = templateRepository.countBySerEmailLayoutIdAndBlIsDeletedFalse(id);
            if (inUse > 0) {
                throw new IllegalArgumentException("Layout is used by " + inUse + " template(s). Remove or reassign them first.");
            }
            CfgTblEmailLayout row = optional.get();
            row.setBlIsDeleted(true);
            row.setBlIsActive(false);
            row.setDteModifiedDate(now);
            if (userId > 0) {
                row.setSerModifiedUserId(userId);
            }
            layoutRepository.save(row);
            deleted++;
        }
        if (deleted == 0) {
            return "{\"status\":\"Failure\",\"message\":\"No layouts deleted\"}";
        }
        return "{\"status\":\"Success\"}";
    }

    @Override
    public List<EmailTemplateDto> getAllTemplates(String category) {
        List<CfgTblEmailTemplate> rows;
        if (category != null && !category.trim().isEmpty()) {
            rows = templateRepository.findByBlIsDeletedFalseAndTxtCategoryIgnoreCaseOrderByIntSortOrderAscTxtTemplateNameAsc(
                    category.trim());
        } else {
            rows = templateRepository.findByBlIsDeletedFalseOrderByIntSortOrderAscTxtTemplateNameAsc();
        }
        Map<Integer, String> layoutNames = loadLayoutNameMap();
        List<EmailTemplateDto> result = new ArrayList<>();
        for (CfgTblEmailTemplate row : rows) {
            result.add(toTemplateDto(row, layoutNames));
        }
        return result;
    }

    @Override
    public EmailTemplateDto getTemplateById(int id) {
        return toTemplateDto(findTemplateOrThrow(id), loadLayoutNameMap());
    }

    @Override
    @Transactional
    public EmailTemplateDto createTemplate(EmailTemplateSaveRequest request, int userId) {
        validateTemplateSave(request, null);
        CfgTblEmailTemplate row = new CfgTblEmailTemplate();
        applyTemplateSave(row, request, userId, true);
        row.setSerCreatedUserId(userId > 0 ? userId : null);
        row.setDteCreatedDate(new Timestamp(System.currentTimeMillis()));
        templateRepository.save(row);
        return toTemplateDto(row, loadLayoutNameMap());
    }

    @Override
    @Transactional
    public EmailTemplateDto updateTemplate(EmailTemplateSaveRequest request, int userId) {
        if (request.getId() == null || request.getId() <= 0) {
            throw new IllegalArgumentException("id is required for update");
        }
        CfgTblEmailTemplate row = findTemplateOrThrow(request.getId());
        validateTemplateSave(request, row.getSerEmailTemplateId());
        applyTemplateSave(row, request, userId, false);
        templateRepository.save(row);
        return toTemplateDto(row, loadLayoutNameMap());
    }

    @Override
    @Transactional
    public String deleteTemplatesByIds(List<Integer> ids, int userId) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("No template id(s) provided");
        }
        int deleted = 0;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (Integer id : ids) {
            if (id == null || id <= 0) {
                continue;
            }
            Optional<CfgTblEmailTemplate> optional = templateRepository.findBySerEmailTemplateIdAndBlIsDeletedFalse(id);
            if (!optional.isPresent()) {
                continue;
            }
            CfgTblEmailTemplate row = optional.get();
            row.setBlIsDeleted(true);
            row.setBlIsActive(false);
            row.setDteModifiedDate(now);
            if (userId > 0) {
                row.setSerModifiedUserId(userId);
            }
            templateRepository.save(row);
            deleted++;
        }
        if (deleted == 0) {
            return "{\"status\":\"Failure\",\"message\":\"No templates deleted\"}";
        }
        return "{\"status\":\"Success\"}";
    }

    private Map<Integer, String> loadLayoutNameMap() {
        Map<Integer, String> map = new HashMap<>();
        for (CfgTblEmailLayout layout : layoutRepository.findByBlIsDeletedFalseOrderByTxtLayoutNameAsc()) {
            map.put(layout.getSerEmailLayoutId(), layout.getTxtLayoutName());
        }
        return map;
    }

    private CfgTblEmailLayout findLayoutOrThrow(int id) {
        return layoutRepository.findBySerEmailLayoutIdAndBlIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Layout not found: " + id));
    }

    private CfgTblEmailTemplate findTemplateOrThrow(int id) {
        return templateRepository.findBySerEmailTemplateIdAndBlIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
    }

    private void validateLayoutSave(EmailLayoutSaveRequest request, Integer existingId) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String code = normalizeCode(request.getLayoutCode());
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Layout code must be uppercase letters, numbers, underscore (e.g. DEFAULT_QARSHI)");
        }
        if (isBlank(request.getLayoutName())) {
            throw new IllegalArgumentException("Layout name is required");
        }
        if (isBlank(request.getHtmlWrapper())) {
            throw new IllegalArgumentException("HTML wrapper is required");
        }
        if (!request.getHtmlWrapper().contains("{{content}}")) {
            throw new IllegalArgumentException("HTML wrapper must contain {{content}} placeholder");
        }
        if (layoutRepository.existsByTxtLayoutCodeIgnoreCaseAndBlIsDeletedFalseAndSerEmailLayoutIdNot(code, existingId == null ? -1 : existingId)) {
            throw new IllegalArgumentException("Layout code already exists: " + code);
        }
        request.setLayoutCode(code);
    }

    private void validateTemplateSave(EmailTemplateSaveRequest request, Integer existingId) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        String code = normalizeCode(request.getTemplateCode());
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Template code must be uppercase letters, numbers, underscore (e.g. USER_WELCOME)");
        }
        if (isBlank(request.getTemplateName())) {
            throw new IllegalArgumentException("Template name is required");
        }
        if (isBlank(request.getSubject())) {
            throw new IllegalArgumentException("Subject is required");
        }
        if (isBlank(request.getBodyHtml())) {
            throw new IllegalArgumentException("Body HTML is required");
        }
        if (templateRepository.existsByTxtTemplateCodeIgnoreCaseAndBlIsDeletedFalseAndSerEmailTemplateIdNot(
                code, existingId == null ? -1 : existingId)) {
            throw new IllegalArgumentException("Template code already exists: " + code);
        }
        if (request.getLayoutId() != null && request.getLayoutId() > 0) {
            layoutRepository.findBySerEmailLayoutIdAndBlIsDeletedFalse(request.getLayoutId())
                    .orElseThrow(() -> new IllegalArgumentException("Layout not found: " + request.getLayoutId()));
        }
        request.setTemplateCode(code);
    }

    private void applyLayoutSave(CfgTblEmailLayout row, EmailLayoutSaveRequest request, int userId, boolean isCreate) {
        row.setTxtLayoutCode(request.getLayoutCode());
        row.setTxtLayoutName(trim(request.getLayoutName()));
        row.setTxtDescription(trimToNull(request.getDescription()));
        row.setTxtHtmlWrapper(request.getHtmlWrapper());
        row.setBlIsActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        row.setBlIsDeleted(false);
        row.setBlnStatus(true);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        if (!isCreate && userId > 0) {
            row.setSerModifiedUserId(userId);
        }
    }

    private void applyTemplateSave(CfgTblEmailTemplate row, EmailTemplateSaveRequest request, int userId, boolean isCreate) {
        row.setTxtTemplateCode(request.getTemplateCode());
        row.setTxtTemplateName(trim(request.getTemplateName()));
        row.setTxtDescription(trimToNull(request.getDescription()));
        row.setTxtSubject(trim(request.getSubject()));
        row.setTxtBodyHtml(request.getBodyHtml());
        if (request.getLayoutId() != null && request.getLayoutId() > 0) {
            row.setSerEmailLayoutId(request.getLayoutId());
        } else {
            row.setSerEmailLayoutId(null);
        }
        row.setTxtPlaceholderHelp(trimToNull(request.getPlaceholderHelp()));
        row.setTxtCategory(trimToNull(request.getCategory()));
        row.setIntSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 100);
        row.setBlIsActive(request.getActive() == null || Boolean.TRUE.equals(request.getActive()));
        row.setBlIsDeleted(false);
        row.setBlnStatus(true);
        row.setDteModifiedDate(new Timestamp(System.currentTimeMillis()));
        if (!isCreate && userId > 0) {
            row.setSerModifiedUserId(userId);
        }
    }

    private EmailLayoutDto toLayoutDto(CfgTblEmailLayout row) {
        EmailLayoutDto dto = new EmailLayoutDto();
        dto.setId(row.getSerEmailLayoutId());
        dto.setLayoutCode(row.getTxtLayoutCode());
        dto.setLayoutName(row.getTxtLayoutName());
        dto.setDescription(row.getTxtDescription());
        dto.setHtmlWrapper(row.getTxtHtmlWrapper());
        dto.setActive(row.isBlIsActive());
        return dto;
    }

    private EmailTemplateDto toTemplateDto(CfgTblEmailTemplate row, Map<Integer, String> layoutNames) {
        EmailTemplateDto dto = new EmailTemplateDto();
        dto.setId(row.getSerEmailTemplateId());
        dto.setTemplateCode(row.getTxtTemplateCode());
        dto.setTemplateName(row.getTxtTemplateName());
        dto.setDescription(row.getTxtDescription());
        dto.setSubject(row.getTxtSubject());
        dto.setBodyHtml(row.getTxtBodyHtml());
        dto.setLayoutId(row.getSerEmailLayoutId());
        if (row.getSerEmailLayoutId() != null && layoutNames != null) {
            dto.setLayoutName(layoutNames.get(row.getSerEmailLayoutId()));
        }
        dto.setPlaceholderHelp(row.getTxtPlaceholderHelp());
        dto.setCategory(row.getTxtCategory());
        dto.setSortOrder(row.getIntSortOrder());
        dto.setActive(row.isBlIsActive());
        return dto;
    }

    private static String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.trim().toUpperCase().replace(' ', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
