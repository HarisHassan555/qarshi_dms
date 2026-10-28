package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.utility.common.UserDelegationAttachmentStorageUtil;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationAttachmentDownload;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegatableDocumentDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationOnBehalfListItemDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationSaveRequest;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobService;
import com.bezkoder.spring.login.backgroundjob.dto.DelegationNotifyEmailJobPayload;
import com.bezkoder.spring.login.sa.bll.services.IUserDelegationService;
import com.bezkoder.spring.login.sa.dal.UserDelegationRepository;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblUserDelegation;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDelegationService implements IUserDelegationService {

    private static final Logger logger = LogManager.getLogger(UserDelegationService.class);

    @Autowired
    private UserDelegationRepository delegationRepository;

    @Autowired
    private IUserService userService;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDelegationEmailNotifier delegationEmailNotifier;

    @Autowired
    private BackgroundJobService backgroundJobService;

    @Override
    @Transactional(readOnly = true)
    public List<UserDelegationDto> getAll(String status, Integer delegateUserId, Integer onBehalfUserId) {
        String statusFilter = normalizeStatusFilter(status);
        List<CfgTblUserDelegation> rows = delegationRepository.findAllNotDeleted().stream()
                .filter(row -> delegateUserId == null || delegateUserId <= 0
                        || delegateUserId.equals(row.getSerDelegateUserId()))
                .filter(row -> onBehalfUserId == null || onBehalfUserId <= 0
                        || onBehalfUserId.equals(row.getSerOnBehalfUserId()))
                .filter(row -> matchesDelegationStatusFilter(row, statusFilter))
                .collect(Collectors.toList());
        Map<Integer, String> templateNamesByFormId = loadTemplateNamesByFormIds(collectFormIdsFromDelegations(rows));
        return rows.stream()
                .map(row -> refreshAndMapRow(row, templateNamesByFormId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDelegationDto getById(Integer id) {
        CfgTblUserDelegation row = delegationRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delegation not found"));
        return refreshAndMapRow(row, loadTemplateNamesByFormIds(collectFormIdsFromScope(row.getTxtScope())));
    }

    @Override
    @Transactional
    public UserDelegationDto create(UserDelegationSaveRequest request, int actorUserId) {
        validateSaveRequest(request);
        CfgTblUserDelegation entity = new CfgTblUserDelegation();
        applyRequestToEntity(entity, request);
        entity.setSerCreatedUserId(actorUserId > 0 ? actorUserId : null);
        entity.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
        entity.setBlIsDeleted(false);
        entity.setBlnStatus(true);
        if (!"REVOKED".equalsIgnoreCase(entity.getTxtStatus())) {
            entity.setTxtStatus(resolveLifecycleStatus(entity.getDteValidFrom(), entity.getDteValidTo()));
        }
        CfgTblUserDelegation saved = delegationRepository.save(entity);
        applyAttachmentChanges(saved, request);
        saved = delegationRepository.save(saved);
        UserDelegationDto dto = toDto(saved);
        maybeNotifyBothParties(saved, request, false);
        return dto;
    }

    @Override
    @Transactional
    public UserDelegationDto update(UserDelegationSaveRequest request, int actorUserId) {
        if (request.getSerDelegationId() == null || request.getSerDelegationId() <= 0) {
            throw new IllegalArgumentException("serDelegationId is required for update");
        }
        validateSaveRequest(request);
        CfgTblUserDelegation entity = delegationRepository.findActiveById(request.getSerDelegationId())
                .orElseThrow(() -> new IllegalArgumentException("Delegation not found"));
        if ("REVOKED".equalsIgnoreCase(entity.getTxtStatus())) {
            throw new IllegalArgumentException("Revoked delegations cannot be edited");
        }
        applyRequestToEntity(entity, request);
        entity.setSerModifiedUserId(actorUserId > 0 ? actorUserId : null);
        entity.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
        entity.setTxtStatus(resolveLifecycleStatus(entity.getDteValidFrom(), entity.getDteValidTo()));
        applyAttachmentChanges(entity, request);
        CfgTblUserDelegation saved = delegationRepository.save(entity);
        UserDelegationDto dto = toDto(saved);
        maybeNotifyBothParties(saved, request, true);
        return dto;
    }

    private void maybeNotifyBothParties(CfgTblUserDelegation saved, UserDelegationSaveRequest request, boolean updated) {
        if (saved == null) {
            return;
        }
        if (!shouldNotifyBothParties(request, updated)) {
            logger.info("Delegation email skipped (notifyBothParties=false) for delegationId="
                    + saved.getSerDelegationId());
            return;
        }
        String documentsLabel = buildDocumentsLabel(saved.getTxtScope());
        if (backgroundJobService != null && backgroundJobService.isEnabled()) {
            DelegationNotifyEmailJobPayload jobPayload = new DelegationNotifyEmailJobPayload();
            jobPayload.setSerDelegationId(saved.getSerDelegationId());
            jobPayload.setUpdated(updated);
            jobPayload.setDocumentsLabel(documentsLabel);
            Integer actorId = commonService.getCurrentLoggedInUser();
            Integer triggeredBy = actorId != null && actorId > 0 ? actorId : null;
            try {
                backgroundJobService.enqueueDelegationNotifyEmail(jobPayload, triggeredBy);
                return;
            } catch (Exception queueEx) {
                logger.warn("Could not queue DELEGATION_NOTIFY_EMAIL for delegationId="
                        + saved.getSerDelegationId() + ", sending synchronously: " + queueEx.getMessage(), queueEx);
            }
        }
        CfgTblUser delegate = userService.getUserById(saved.getSerDelegateUserId());
        CfgTblUser onBehalf = userService.getUserById(saved.getSerOnBehalfUserId());
        delegationEmailNotifier.notifyBothParties(saved, delegate, onBehalf, documentsLabel, updated);
    }

    private boolean shouldNotifyBothParties(UserDelegationSaveRequest request, boolean updated) {
        if (request == null) {
            return false;
        }
        Boolean notify = request.getNotifyBothParties();
        if (Boolean.TRUE.equals(notify)) {
            return true;
        }
        if (Boolean.FALSE.equals(notify)) {
            return false;
        }
        // New delegations: default notify when the client omits the flag (older FE builds).
        return !updated;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDelegatableDocumentDto> getDelegatableDocumentsForOnBehalfUser(Integer onBehalfUserId) {
        if (onBehalfUserId == null || onBehalfUserId <= 0) {
            throw new IllegalArgumentException("On-behalf user is required");
        }
        CfgTblUser onBehalf = userService.getUserById(onBehalfUserId);
        if (onBehalf == null || Boolean.TRUE.equals(onBehalf.getBlIsDeleted())) {
            throw new IllegalArgumentException("On-behalf user not found");
        }

        List<TemplateDefinition> definitions = entityManager.createQuery(
                "SELECT t FROM TemplateDefinition t WHERE (t.blIsDeleted IS NULL OR t.blIsDeleted = false) "
                        + "ORDER BY t.txtTemplateName ASC",
                TemplateDefinition.class)
                .getResultList();

        List<UserDelegatableDocumentDto> documents = new ArrayList<>();
        for (TemplateDefinition definition : definitions) {
            if (!isDelegatableTemplateForOnBehalfUser(definition, onBehalfUserId)) {
                continue;
            }
            UserDelegatableDocumentDto dto = new UserDelegatableDocumentDto();
            dto.setSerFormId(definition.getSerFormId());
            dto.setTxtTemplateName(definition.getTxtTemplateName());
            dto.setTxtCodeConvention(definition.getTxtCodeConvention());
            documents.add(dto);
        }
        documents.sort(Comparator.comparing(
                (UserDelegatableDocumentDto row) -> row.getTxtTemplateName() != null ? row.getTxtTemplateName() : "",
                String.CASE_INSENSITIVE_ORDER));
        return documents;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelegateAccessTemplateDefinition(Integer delegateUserId, TemplateDefinition definition) {
        if (delegateUserId == null || delegateUserId <= 0 || definition == null || definition.getSerFormId() == null) {
            return false;
        }
        return !getDelegationOnBehalfForTemplate(delegateUserId, definition.getSerFormId()).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDelegationOnBehalfListItemDto> getDelegationOnBehalfForTemplate(
            Integer delegateUserId, Integer formId) {
        if (delegateUserId == null || delegateUserId <= 0 || formId == null || formId <= 0) {
            return Collections.emptyList();
        }
        TemplateDefinition definition = findTemplateByFormId(formId);
        if (definition == null) {
            return Collections.emptyList();
        }
        LinkedHashSet<Integer> seenOnBehalfIds = new LinkedHashSet<>();
        List<UserDelegationOnBehalfListItemDto> items = new ArrayList<>();
        for (CfgTblUserDelegation delegation : findEffectiveDelegationsForDelegate(delegateUserId)) {
            if (!scopeContainsFormId(delegation.getTxtScope(), formId)) {
                continue;
            }
            Integer onBehalfUserId = delegation.getSerOnBehalfUserId();
            if (onBehalfUserId == null || onBehalfUserId <= 0 || seenOnBehalfIds.contains(onBehalfUserId)) {
                continue;
            }
            if (!isDelegatableTemplateForOnBehalfUser(definition, onBehalfUserId)) {
                continue;
            }
            seenOnBehalfIds.add(onBehalfUserId);
            UserDelegationOnBehalfListItemDto item = new UserDelegationOnBehalfListItemDto();
            item.setSerOnBehalfUserId(onBehalfUserId);
            CfgTblUser onBehalf = userService.getUserById(onBehalfUserId);
            if (onBehalf != null && onBehalf.getTxtUserName() != null && !onBehalf.getTxtUserName().trim().isEmpty()) {
                item.setOnBehalfUserName(onBehalf.getTxtUserName().trim());
            } else {
                item.setOnBehalfUserName("User #" + onBehalfUserId);
            }
            items.add(item);
        }
        return items;
    }

    private List<CfgTblUserDelegation> findEffectiveDelegationsForDelegate(Integer delegateUserId) {
        List<CfgTblUserDelegation> rows = delegationRepository.findDelegationsForDelegate(delegateUserId);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDate today = LocalDate.now();
        List<CfgTblUserDelegation> effective = new ArrayList<>();
        for (CfgTblUserDelegation row : rows) {
            if (isDelegationEffectiveOn(row, today)) {
                effective.add(row);
            }
        }
        return effective;
    }

    private boolean isDelegationEffectiveOn(CfgTblUserDelegation row, LocalDate today) {
        if (row == null || row.getDteValidFrom() == null || row.getDteValidTo() == null) {
            return false;
        }
        LocalDate from = row.getDteValidFrom().toLocalDate();
        LocalDate to = row.getDteValidTo().toLocalDate();
        if (today.isBefore(from) || today.isAfter(to)) {
            return false;
        }
        String status = row.getTxtStatus() != null ? row.getTxtStatus().trim().toUpperCase(Locale.ROOT) : "";
        return !"REVOKED".equals(status);
    }

    private boolean scopeContainsFormId(String scopeCsv, int formId) {
        for (String id : parseScopeIds(scopeCsv)) {
            try {
                if (Integer.parseInt(id) == formId) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDelegationAttachmentDownload loadAttachmentDownload(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Delegation id is required");
        }
        CfgTblUserDelegation row = delegationRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delegation not found"));
        File file = UserDelegationAttachmentStorageUtil.resolveAttachmentFile(row.getTxtAttachmentPath());
        if (file == null) {
            throw new IllegalArgumentException("Attachment not found");
        }
        try {
            UserDelegationAttachmentDownload download = new UserDelegationAttachmentDownload();
            download.setContent(Files.readAllBytes(file.toPath()));
            String contentType = Files.probeContentType(file.toPath());
            download.setContentType(contentType != null ? contentType : "application/octet-stream");
            String name = row.getTxtAttachmentName();
            download.setFileName(name != null && !name.trim().isEmpty() ? name.trim() : file.getName());
            return download;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read attachment");
        }
    }

    @Override
    @Transactional
    public UserDelegationDto revoke(Integer id, int actorUserId) {
        CfgTblUserDelegation entity = delegationRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delegation not found"));
        entity.setTxtStatus("REVOKED");
        entity.setDteRevokedDate(commonService.getCurrentTimeStamp_new());
        entity.setSerRevokedByUserId(actorUserId > 0 ? actorUserId : null);
        entity.setSerModifiedUserId(actorUserId > 0 ? actorUserId : null);
        entity.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
        CfgTblUserDelegation saved = delegationRepository.save(entity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id, int actorUserId) {
        CfgTblUserDelegation entity = delegationRepository.findActiveById(id)
                .orElseThrow(() -> new IllegalArgumentException("Delegation not found"));
        entity.setBlIsDeleted(true);
        entity.setBlnStatus(false);
        entity.setSerModifiedUserId(actorUserId > 0 ? actorUserId : null);
        entity.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
        delegationRepository.save(entity);
    }

    private UserDelegationDto refreshAndMapRow(CfgTblUserDelegation row) {
        return refreshAndMapRow(row, loadTemplateNamesByFormIds(collectFormIdsFromScope(row.getTxtScope())));
    }

    private UserDelegationDto refreshAndMapRow(CfgTblUserDelegation row, Map<Integer, String> templateNamesByFormId) {
        if (row == null) {
            return null;
        }
        if (!"REVOKED".equalsIgnoreCase(row.getTxtStatus())) {
            String lifecycle = resolveLifecycleStatus(row.getDteValidFrom(), row.getDteValidTo());
            if (!lifecycle.equalsIgnoreCase(row.getTxtStatus())) {
                row.setTxtStatus(lifecycle);
                delegationRepository.save(row);
            }
        }
        return toDto(row, templateNamesByFormId);
    }

    private Set<Integer> collectFormIdsFromDelegations(List<CfgTblUserDelegation> rows) {
        Set<Integer> formIds = new LinkedHashSet<>();
        if (rows == null) {
            return formIds;
        }
        for (CfgTblUserDelegation row : rows) {
            formIds.addAll(collectFormIdsFromScope(row != null ? row.getTxtScope() : null));
        }
        return formIds;
    }

    private Set<Integer> collectFormIdsFromScope(String scopeCsv) {
        Set<Integer> formIds = new LinkedHashSet<>();
        for (String idToken : parseScopeIds(scopeCsv)) {
            try {
                formIds.add(Integer.parseInt(idToken));
            } catch (NumberFormatException ignored) {
                // skip invalid scope token
            }
        }
        return formIds;
    }

    /**
     * Loads {@code ser_form_id -> txt_template_name} for delegation list display only.
     */
    private Map<Integer, String> loadTemplateNamesByFormIds(Set<Integer> formIds) {
        if (formIds == null || formIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TemplateDefinition> templates = entityManager.createQuery(
                "SELECT t FROM TemplateDefinition t WHERE t.serFormId IN :formIds",
                TemplateDefinition.class)
                .setParameter("formIds", formIds)
                .getResultList();
        Map<Integer, String> names = new HashMap<>();
        for (TemplateDefinition template : templates) {
            if (template.getSerFormId() == null || template.getTxtTemplateName() == null) {
                continue;
            }
            String label = template.getTxtTemplateName().trim();
            if (!label.isEmpty()) {
                names.put(template.getSerFormId(), label);
            }
        }
        return names;
    }

    private void applyRequestToEntity(CfgTblUserDelegation entity, UserDelegationSaveRequest request) {
        entity.setSerDelegateUserId(request.getSerDelegateUserId());
        entity.setSerOnBehalfUserId(request.getSerOnBehalfUserId());
        entity.setDteValidFrom(parseDate(request.getDteValidFrom()));
        entity.setDteValidTo(parseDate(request.getDteValidTo()));
        entity.setTxtScope(normalizeScope(request));
        entity.setTxtNotes(trimToNull(request.getTxtNotes()));
    }

    private void validateSaveRequest(UserDelegationSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getSerDelegateUserId() == null || request.getSerDelegateUserId() <= 0) {
            throw new IllegalArgumentException("Delegate user is required");
        }
        if (request.getSerOnBehalfUserId() == null || request.getSerOnBehalfUserId() <= 0) {
            throw new IllegalArgumentException("On-behalf user is required");
        }
        if (request.getSerDelegateUserId().equals(request.getSerOnBehalfUserId())) {
            throw new IllegalArgumentException("Delegate and on-behalf user must be different");
        }
        if (request.getDteValidFrom() == null || request.getDteValidFrom().trim().isEmpty()) {
            throw new IllegalArgumentException("Valid from is required");
        }
        if (request.getDteValidTo() == null || request.getDteValidTo().trim().isEmpty()) {
            throw new IllegalArgumentException("Valid to is required");
        }
        Date from = parseDate(request.getDteValidFrom());
        Date to = parseDate(request.getDteValidTo());
        if (to.before(from)) {
            throw new IllegalArgumentException("Valid to must be on or after valid from");
        }

        CfgTblUser delegate = userService.getUserById(request.getSerDelegateUserId());
        if (delegate == null || Boolean.TRUE.equals(delegate.getBlIsDeleted())) {
            throw new IllegalArgumentException("Delegate user not found");
        }
        CfgTblUser onBehalf = userService.getUserById(request.getSerOnBehalfUserId());
        if (onBehalf == null || Boolean.TRUE.equals(onBehalf.getBlIsDeleted())) {
            throw new IllegalArgumentException("On-behalf user not found");
        }

        String scope = normalizeScope(request);
        if (scope.isEmpty()) {
            throw new IllegalArgumentException("At least one published document is required");
        }
        validateScopeForOnBehalfUser(scope, request.getSerOnBehalfUserId());
        validateAttachmentPayload(request);
    }

    private void validateAttachmentPayload(UserDelegationSaveRequest request) {
        if (request.getAttachmentBase64() == null || request.getAttachmentBase64().trim().isEmpty()) {
            return;
        }
        String base64 = request.getAttachmentBase64().trim();
        if (base64.length() > 14_000_000) {
            throw new IllegalArgumentException("Attachment is too large (max 10 MB)");
        }
    }

    private void applyAttachmentChanges(CfgTblUserDelegation entity, UserDelegationSaveRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (Boolean.TRUE.equals(request.getRemoveAttachment())) {
            clearAttachment(entity);
            return;
        }
        String base64 = request.getAttachmentBase64();
        if (base64 == null || base64.trim().isEmpty()) {
            return;
        }
        Integer delegationId = entity.getSerDelegationId();
        if (delegationId == null || delegationId <= 0) {
            throw new IllegalStateException("Delegation id is required before saving attachment");
        }
        try {
            String previousPath = entity.getTxtAttachmentPath();
            String path = UserDelegationAttachmentStorageUtil.saveDelegationAttachment(
                    delegationId,
                    base64,
                    request.getAttachmentFileName());
            if (path == null) {
                throw new IllegalArgumentException("Could not save attachment");
            }
            if (previousPath != null && !previousPath.equals(path)) {
                UserDelegationAttachmentStorageUtil.deleteAttachmentFile(previousPath);
            }
            entity.setTxtAttachmentPath(path);
            String displayName = UserDelegationAttachmentStorageUtil.sanitizeDisplayFileName(
                    request.getAttachmentFileName());
            if (displayName == null) {
                displayName = "delegation-" + delegationId + "-attachment";
            }
            entity.setTxtAttachmentName(displayName);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not save attachment: " + ex.getMessage());
        }
    }

    private void clearAttachment(CfgTblUserDelegation entity) {
        if (entity.getTxtAttachmentPath() != null) {
            UserDelegationAttachmentStorageUtil.deleteAttachmentFile(entity.getTxtAttachmentPath());
        }
        entity.setTxtAttachmentPath(null);
        entity.setTxtAttachmentName(null);
    }

    private void validateScopeForOnBehalfUser(String scopeCsv, Integer onBehalfUserId) {
        for (String token : scopeCsv.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int formId;
            try {
                formId = Integer.parseInt(trimmed);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid document id in scope: " + trimmed);
            }
            TemplateDefinition definition = findTemplateByFormId(formId);
            if (definition == null) {
                throw new IllegalArgumentException("Document not found for form id: " + formId);
            }
            if (Boolean.TRUE.equals(definition.getBlIsDeleted())) {
                throw new IllegalArgumentException("Document is deleted for form id: " + formId);
            }
            if (!isDelegatableTemplateForOnBehalfUser(definition, onBehalfUserId)) {
                String templateName = definition.getTxtTemplateName() != null
                        ? definition.getTxtTemplateName()
                        : ("Form #" + formId);
                throw new IllegalArgumentException(
                        "Document \"" + templateName + "\" is not published, active, or visible to the on-behalf user");
            }
        }
    }

    /**
     * Same rules as Digital Document List for a non-admin user: published, active, and visibility list (empty = all).
     */
    private boolean isDelegatableTemplateForOnBehalfUser(TemplateDefinition definition, Integer onBehalfUserId) {
        if (definition == null || onBehalfUserId == null || onBehalfUserId <= 0) {
            return false;
        }
        if (Boolean.TRUE.equals(definition.getBlIsDeleted())) {
            return false;
        }
        if (isTemplateDefinitionDraft(definition)) {
            return false;
        }
        if (!isTemplateDefinitionActive(definition)) {
            return false;
        }
        Set<Integer> visibleUserIds = extractVisibleUserIds(definition);
        if (visibleUserIds.isEmpty()) {
            return true;
        }
        return visibleUserIds.contains(onBehalfUserId);
    }

    private boolean isTemplateDefinitionActive(TemplateDefinition definition) {
        if (definition == null) {
            return false;
        }
        if (isTemplateDefinitionDraft(definition)) {
            return false;
        }
        if (Boolean.FALSE.equals(definition.getBlIsActive()) || Boolean.FALSE.equals(definition.getBlnStatus())) {
            return false;
        }
        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        return isTemplatePayloadMarkedActive(payload);
    }

    private boolean isTemplatePayloadMarkedActive(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return true;
        }
        Object explicitIsActive = payload.get("isActive");
        if (explicitIsActive != null) {
            return !isFalseValue(explicitIsActive);
        }
        Object explicitStatus = payload.get("blnStatus");
        if (explicitStatus != null) {
            return !isFalseValue(explicitStatus);
        }
        Object explicitActiveFlag = payload.get("blIsActive");
        if (explicitActiveFlag != null) {
            return !isFalseValue(explicitActiveFlag);
        }
        return true;
    }

    private boolean isFalseValue(Object value) {
        if (value instanceof Boolean) {
            return !((Boolean) value);
        }
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return "false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized);
    }

    private Set<Integer> extractVisibleUserIds(TemplateDefinition definition) {
        Set<Integer> userIds = new LinkedHashSet<>();
        if (definition == null || definition.getTxtTemplatePayload() == null
                || definition.getTxtTemplatePayload().trim().isEmpty()) {
            return userIds;
        }
        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        addVisibleUserIds(userIds, payload.get("visibleUserIds"));
        addVisibleUserIds(userIds, payload.get("visibilityUserIds"));
        addVisibleUserIds(userIds, payload.get("selectedUsers"));
        return userIds;
    }

    private void addVisibleUserIds(Set<Integer> target, Object value) {
        if (!(value instanceof List)) {
            return;
        }
        for (Object item : (List<?>) value) {
            Integer userId = toInteger(item);
            if (userId != null && userId > 0) {
                target.add(userId);
            }
        }
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private TemplateDefinition findTemplateByFormId(int formId) {
        try {
            return entityManager.createQuery(
                    "SELECT t FROM TemplateDefinition t WHERE t.serFormId = :formId",
                    TemplateDefinition.class)
                    .setParameter("formId", formId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    private boolean isTemplateDefinitionDraft(TemplateDefinition definition) {
        if (definition == null) {
            return false;
        }
        Map<String, Object> payload = parseMap(definition.getTxtTemplatePayload());
        if (payload == null || payload.isEmpty()) {
            return false;
        }
        Object draftFlag = payload.get("isDraft");
        if (draftFlag instanceof Boolean) {
            return (Boolean) draftFlag;
        }
        return draftFlag != null && "true".equalsIgnoreCase(String.valueOf(draftFlag).trim());
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeScope(UserDelegationSaveRequest request) {
        Set<String> ids = new LinkedHashSet<>();
        if (request.getDocumentIds() != null) {
            for (String id : request.getDocumentIds()) {
                if (id != null && !id.trim().isEmpty()) {
                    ids.add(id.trim());
                }
            }
        }
        if (ids.isEmpty() && request.getTxtScope() != null) {
            for (String part : request.getTxtScope().split(",")) {
                if (part != null && !part.trim().isEmpty()) {
                    ids.add(part.trim());
                }
            }
        }
        return ids.stream().collect(Collectors.joining(","));
    }

    private UserDelegationDto toDto(CfgTblUserDelegation entity) {
        return toDto(entity, loadTemplateNamesByFormIds(collectFormIdsFromScope(entity.getTxtScope())));
    }

    private UserDelegationDto toDto(CfgTblUserDelegation entity, Map<Integer, String> templateNamesByFormId) {
        UserDelegationDto dto = new UserDelegationDto();
        dto.setSerDelegationId(entity.getSerDelegationId());
        dto.setSerDelegateUserId(entity.getSerDelegateUserId());
        dto.setSerOnBehalfUserId(entity.getSerOnBehalfUserId());
        dto.setDteValidFrom(entity.getDteValidFrom() != null ? entity.getDteValidFrom().toString() : null);
        dto.setDteValidTo(entity.getDteValidTo() != null ? entity.getDteValidTo().toString() : null);
        dto.setTxtScope(entity.getTxtScope());
        dto.setDocumentIds(parseScopeIds(entity.getTxtScope()));
        dto.setTxtStatus(entity.getTxtStatus());
        dto.setTxtNotes(entity.getTxtNotes());
        boolean hasAttachment = entity.getTxtAttachmentPath() != null
                && !entity.getTxtAttachmentPath().trim().isEmpty()
                && UserDelegationAttachmentStorageUtil.resolveAttachmentFile(entity.getTxtAttachmentPath()) != null;
        dto.setHasAttachment(hasAttachment);
        dto.setAttachmentFileName(hasAttachment ? entity.getTxtAttachmentName() : null);

        CfgTblUser delegate = userService.getUserById(entity.getSerDelegateUserId());
        if (delegate != null) {
            dto.setDelegateUserName(delegate.getTxtUserName());
            dto.setDelegateEmployeeId(delegate.getTxtCnic());
        }
        CfgTblUser onBehalf = userService.getUserById(entity.getSerOnBehalfUserId());
        if (onBehalf != null) {
            dto.setOnBehalfUserName(onBehalf.getTxtUserName());
            if (onBehalf.getCfgTblRole() != null) {
                dto.setOnBehalfRole(onBehalf.getCfgTblRole().getTxtRoleName());
            }
        }
        if (entity.getSerCreatedUserId() != null) {
            CfgTblUser creator = userService.getUserById(entity.getSerCreatedUserId());
            if (creator != null) {
                dto.setCreatedByName(creator.getTxtLoginName() != null
                        ? creator.getTxtLoginName()
                        : creator.getTxtUserName());
            }
        }
        dto.setDocumentsLabel(buildTemplateNamesLabel(entity.getTxtScope(), templateNamesByFormId));
        dto.setScopedDocuments(Collections.emptyList());
        return dto;
    }

    /** Comma-separated template names for ids in delegation scope only. */
    private String buildTemplateNamesLabel(String scopeCsv, Map<Integer, String> templateNamesByFormId) {
        if (templateNamesByFormId == null || templateNamesByFormId.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (String idToken : parseScopeIds(scopeCsv)) {
            try {
                int formId = Integer.parseInt(idToken);
                String templateName = templateNamesByFormId.get(formId);
                if (templateName != null && !templateName.trim().isEmpty()) {
                    names.add(templateName.trim());
                }
            } catch (NumberFormatException ignored) {
                // skip invalid scope token
            }
        }
        return names.isEmpty() ? "" : String.join(", ", names);
    }

    private String buildDocumentsLabel(String scopeCsv) {
        return buildTemplateNamesLabel(scopeCsv, loadTemplateNamesByFormIds(collectFormIdsFromScope(scopeCsv)));
    }

    private List<String> parseScopeIds(String scopeCsv) {
        List<String> ids = new ArrayList<>();
        if (scopeCsv == null) {
            return ids;
        }
        for (String part : scopeCsv.split(",")) {
            if (part != null && !part.trim().isEmpty()) {
                ids.add(part.trim());
            }
        }
        return ids;
    }

    private String resolveLifecycleStatus(Date from, Date to) {
        LocalDate today = LocalDate.now();
        LocalDate start = from.toLocalDate();
        LocalDate end = to.toLocalDate();
        if (today.isAfter(end)) {
            return "EXPIRED";
        }
        if (today.isBefore(start)) {
            return "SCHEDULED";
        }
        return "ACTIVE";
    }

    private Date parseDate(String value) {
        try {
            return Date.valueOf(LocalDate.parse(value.trim()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid date: " + value);
        }
    }

    private boolean matchesDelegationStatusFilter(CfgTblUserDelegation row, String statusFilter) {
        if (statusFilter == null) {
            return true;
        }
        String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
        if ("REVOKED".equals(normalized)) {
            return "REVOKED".equalsIgnoreCase(row.getTxtStatus());
        }
        if ("ACTIVE".equals(normalized)) {
            return isDelegationEffectiveOn(row, LocalDate.now());
        }
        if ("EXPIRED".equals(normalized)) {
            if (row.getDteValidTo() == null) {
                return false;
            }
            return LocalDate.now().isAfter(row.getDteValidTo().toLocalDate());
        }
        if ("SCHEDULED".equals(normalized)) {
            if (row.getDteValidFrom() == null) {
                return false;
            }
            return LocalDate.now().isBefore(row.getDteValidFrom().toLocalDate());
        }
        return normalized.equalsIgnoreCase(row.getTxtStatus() != null ? row.getTxtStatus() : "");
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
