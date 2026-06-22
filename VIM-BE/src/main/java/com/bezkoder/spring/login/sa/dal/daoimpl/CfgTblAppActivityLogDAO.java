package com.bezkoder.spring.login.sa.dal.daoimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblAppActivityLogDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblAppActivityLog;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CfgTblAppActivityLogDAO implements ICfgTblAppActivityLogDAO {

    private static final Logger log = LoggerFactory.getLogger(CfgTblAppActivityLogDAO.class);

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CfgTblAppActivityLog> getAllActivityLogs() {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            List<CfgTblAppActivityLog> logs = em.createQuery(
                    "FROM CfgTblAppActivityLog ORDER BY dteCreatedDate DESC").getResultList();
            em.getTransaction().commit();
            return logs;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error(e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<CfgTblAppActivityLog> getActivityLogsByFilters(String actionType, String status, Integer userId,
            Integer entityId, String startDate, String endDate) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            StringBuilder jpql = new StringBuilder("FROM CfgTblAppActivityLog WHERE 1=1");
            Map<String, Object> params = new HashMap<>();

            if (actionType != null && !actionType.trim().isEmpty()) {
                jpql.append(" AND txtActionType = :actionType");
                params.put("actionType", actionType.trim().toUpperCase());
            }
            if (status != null && !status.trim().isEmpty()) {
                jpql.append(" AND txtStatus = :status");
                params.put("status", status.trim().toUpperCase());
            }
            if (userId != null && userId > 0) {
                jpql.append(" AND serUserId = :userId");
                params.put("userId", userId);
            }
            if (entityId != null && entityId > 0) {
                jpql.append(" AND serEntityId = :entityId");
                params.put("entityId", entityId);
            }
            if (startDate != null && !startDate.trim().isEmpty()) {
                jpql.append(" AND dteCreatedDate >= :startDate");
                params.put("startDate", parseDateStart(startDate.trim()));
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                jpql.append(" AND dteCreatedDate <= :endDate");
                params.put("endDate", parseDateEnd(endDate.trim()));
            }
            jpql.append(" ORDER BY dteCreatedDate DESC");

            javax.persistence.Query query = em.createQuery(jpql.toString());
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }
            List<CfgTblAppActivityLog> logs = query.getResultList();
            em.getTransaction().commit();
            return logs;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error(e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public CfgTblAppActivityLog getActivityLogById(Integer id) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            CfgTblAppActivityLog logEntry = em.find(CfgTblAppActivityLog.class, id);
            em.getTransaction().commit();
            return logEntry;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error(e.getMessage(), e);
            return null;
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public String addActivityLog(CfgTblAppActivityLog activityLog) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            if (activityLog.getDteCreatedDate() == null) {
                activityLog.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
            }
            em.persist(activityLog);
            em.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error(e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public String updateActivityLog(CfgTblAppActivityLog activityLog, boolean syncToSource) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            CfgTblAppActivityLog existing = em.find(CfgTblAppActivityLog.class, activityLog.getSerActivityLogId());
            if (existing == null) {
                em.getTransaction().rollback();
                return "Failure: Activity log not found";
            }

            if (activityLog.getTxtActionType() != null) {
                existing.setTxtActionType(activityLog.getTxtActionType());
            }
            if (activityLog.getSerUserId() != null) {
                existing.setSerUserId(activityLog.getSerUserId());
            }
            if (activityLog.getTxtUsername() != null) {
                existing.setTxtUsername(activityLog.getTxtUsername());
            }
            if (activityLog.getTxtIpAddress() != null) {
                existing.setTxtIpAddress(activityLog.getTxtIpAddress());
            }
            if (activityLog.getTxtDevice() != null) {
                existing.setTxtDevice(activityLog.getTxtDevice());
            }
            if (activityLog.getDteCreatedDate() != null) {
                existing.setDteCreatedDate(activityLog.getDteCreatedDate());
            }
            if (activityLog.getTxtStatus() != null) {
                existing.setTxtStatus(activityLog.getTxtStatus());
            }
            if (activityLog.getTxtMessage() != null) {
                existing.setTxtMessage(activityLog.getTxtMessage());
            }
            if (activityLog.getSerEntityId() != null) {
                existing.setSerEntityId(activityLog.getSerEntityId());
            }
            if (activityLog.getTxtEntityType() != null) {
                existing.setTxtEntityType(activityLog.getTxtEntityType());
            }
            if (activityLog.getTxtPayload() != null) {
                existing.setTxtPayload(activityLog.getTxtPayload());
            }
            if (activityLog.getTxtErrorMessage() != null) {
                existing.setTxtErrorMessage(activityLog.getTxtErrorMessage());
            }

            em.merge(existing);

            if (syncToSource) {
                String syncResult = syncPayloadToApplicationHistory(em, existing);
                if (syncResult != null && syncResult.startsWith("Failure")) {
                    em.getTransaction().rollback();
                    return syncResult;
                }
            }

            em.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error(e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public Map<String, Object> getTransactionForActivityLog(Integer logId) {
        Map<String, Object> result = new HashMap<>();
        result.put("editable", false);

        if (logId == null || logId <= 0) {
            result.put("message", "Invalid activity log id");
            return result;
        }

        CfgTblAppActivityLog activityLog = getActivityLogById(logId);
        if (activityLog == null) {
            result.put("message", "Activity log not found");
            return result;
        }

        if (!isApplicationTransactionLog(activityLog)) {
            result.put("message", "Only application approval transactions can be edited here");
            result.put("serActivityLogId", activityLog.getSerActivityLogId());
            result.put("txtActionType", activityLog.getTxtActionType());
            return result;
        }

        try {
            ResolvedTransaction ctx = resolveTransactionContext(activityLog);
            if (ctx == null) {
                result.put("message", "Could not locate application transaction for this log");
                return result;
            }

            Map<String, Object> entry = ctx.history.get(ctx.historyIndex);
            result.put("editable", true);
            result.put("serActivityLogId", activityLog.getSerActivityLogId());
            result.put("applicationId", ctx.application.getSerApplicationId());
            result.put("formCode", ctx.application.getTxtFormCode());
            result.put("applicationStatus", ctx.application.getTxtStatus());
            result.put("historyIndex", ctx.historyIndex);
            result.put("txtActionType", activityLog.getTxtActionType());
            result.put("historyAction", stringValue(entry.get("action")));
            result.put("remarks", stringValue(entry.get("remarks")));
            result.put("approvedBy", toInteger(entry.get("approvedBy")));
            result.put("approverName", stringValue(entry.get("approverName")));
            result.put("level", toInteger(entry.get("level")));
            result.put("role", stringValue(entry.get("role")));
            result.put("approvedIp", firstNonBlank(stringValue(entry.get("approvedIp")), activityLog.getTxtIpAddress()));
            result.put("approvedVia", stringValue(entry.get("approvedVia")));
            result.put("approvedDate", firstNonBlank(stringValue(entry.get("approvedDate")), stringValue(entry.get("approvedAt"))));
            result.put("designation", stringValue(entry.get("designation")));
            result.put("departmentName", firstNonBlank(stringValue(entry.get("txtDepartmentName")),
                    stringValue(entry.get("userDepartmentName"))));
            result.put("logUserId", activityLog.getSerUserId());
            result.put("logUsername", activityLog.getTxtUsername());
            result.put("logCreatedDate", activityLog.getDteCreatedDate());
            return result;
        } catch (Exception e) {
            log.error("getTransactionForActivityLog failed: {}", e.getMessage(), e);
            result.put("message", "Failed to load transaction: " + e.getMessage());
            return result;
        }
    }

    @Override
    public String updateTransactionForActivityLog(Integer logId, Map<String, Object> updates) {
        if (logId == null || logId <= 0) {
            return "Failure: Invalid activity log id";
        }
        if (updates == null) {
            return "Failure: No updates provided";
        }

        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();

            CfgTblAppActivityLog activityLog = em.find(CfgTblAppActivityLog.class, logId);
            if (activityLog == null) {
                em.getTransaction().rollback();
                return "Failure: Activity log not found";
            }
            if (!isApplicationTransactionLog(activityLog)) {
                em.getTransaction().rollback();
                return "Failure: Only application approval transactions can be edited";
            }

            ResolvedTransaction ctx = resolveTransactionContext(activityLog, em);
            if (ctx == null) {
                em.getTransaction().rollback();
                return "Failure: Could not locate application transaction for this log";
            }

            Map<String, Object> entry = ctx.history.get(ctx.historyIndex);
            Integer originalApprovedBy = toInteger(entry.get("approvedBy"));
            Integer originalLevel = toInteger(entry.get("level"));
            String originalAction = stringValue(entry.get("action"));
            String originalApprovedDate = firstNonBlank(stringValue(entry.get("approvedDate")),
                    stringValue(entry.get("approvedAt")));

            if (updates.containsKey("historyAction")) {
                entry.put("action", String.valueOf(updates.get("historyAction")));
            }
            if (updates.containsKey("remarks")) {
                entry.put("remarks", String.valueOf(updates.get("remarks")));
            }
            if (updates.containsKey("approvedIp")) {
                entry.put("approvedIp", String.valueOf(updates.get("approvedIp")));
            }
            if (updates.containsKey("approvedVia")) {
                entry.put("approvedVia", String.valueOf(updates.get("approvedVia")));
            }
            if (updates.containsKey("approvedDate")) {
                String approvedDate = String.valueOf(updates.get("approvedDate"));
                entry.put("approvedDate", approvedDate);
                entry.put("approvedAt", approvedDate);
            }
            if (updates.containsKey("level")) {
                entry.put("level", toInteger(updates.get("level")));
            }
            if (updates.containsKey("role")) {
                entry.put("role", String.valueOf(updates.get("role")));
            }

            Integer approvedBy = updates.containsKey("approvedBy")
                    ? toInteger(updates.get("approvedBy"))
                    : toInteger(entry.get("approvedBy"));
            if (approvedBy != null && approvedBy > 0) {
                entry.put("approvedBy", approvedBy);
                CfgTblUser approver = commonService.getCurrentUser(approvedBy);
                if (approver != null) {
                    entry.put("approverName", approver.getTxtUserName());
                    if (approver.getTxtDesignation() != null) {
                        entry.put("designation", approver.getTxtDesignation());
                        entry.put("txtDesignation", approver.getTxtDesignation());
                    }
                    if (approver.getTxtDepartmentName() != null) {
                        entry.put("txtDepartmentName", approver.getTxtDepartmentName());
                        entry.put("userDepartmentName", approver.getTxtDepartmentName());
                    }
                    if (approver.getTxtSignaturePath() != null) {
                        entry.put("signaturePath", approver.getTxtSignaturePath());
                    }
                    activityLog.setSerUserId(approvedBy);
                    activityLog.setTxtUsername(approver.getTxtUserName());
                }
            }

            ctx.application.setTxtApprovalHistory(objectMapper.writeValueAsString(ctx.history));
            if (updates.containsKey("remarks")) {
                ctx.application.setTxtRemarks(String.valueOf(updates.get("remarks")));
            }
            syncPriorApprovalsEntry(ctx.application, originalApprovedBy, originalLevel, originalAction,
                    originalApprovedDate, entry);
            if (originalApprovedBy != null && approvedBy != null && approvedBy > 0
                    && !originalApprovedBy.equals(approvedBy)) {
                syncApplicationFormDataForApproverChange(ctx.application, originalApprovedBy, approvedBy, entry, em);
                if (originalApprovedBy.equals(ctx.application.getSerCurrentApprover())) {
                    ctx.application.setSerCurrentApprover(approvedBy);
                }
            }
            em.merge(ctx.application);

            String historyAction = stringValue(entry.get("action"));
            activityLog.setTxtActionType(mapHistoryActionToLogAction(historyAction));
            activityLog.setTxtMessage(buildTransactionMessage(historyAction));
            if (updates.containsKey("approvedIp")) {
                activityLog.setTxtIpAddress(String.valueOf(updates.get("approvedIp")));
            }

            Map<String, Object> payload = readPayloadMap(activityLog.getTxtPayload());
            payload.put("targetType", "APPROVAL_HISTORY");
            payload.put("applicationId", ctx.application.getSerApplicationId());
            payload.put("historyIndex", ctx.historyIndex);
            payload.put("level", entry.get("level"));
            payload.put("role", entry.get("role"));
            payload.put("action", historyAction);
            payload.put("remarks", entry.get("remarks"));
            payload.put("approvedIp", entry.get("approvedIp"));
            payload.put("approvedVia", entry.get("approvedVia"));
            activityLog.setTxtPayload(objectMapper.writeValueAsString(payload));
            activityLog.setSerEntityId(ctx.application.getSerApplicationId());
            activityLog.setTxtEntityType("APPLICATION");

            em.merge(activityLog);
            em.getTransaction().commit();
            return "Success";
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error("updateTransactionForActivityLog failed: {}", e.getMessage(), e);
            return "Failure: " + e.getMessage();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private boolean isApplicationTransactionLog(CfgTblAppActivityLog activityLog) {
        if (activityLog == null) {
            return false;
        }
        String action = activityLog.getTxtActionType() != null ? activityLog.getTxtActionType().toUpperCase() : "";
        return "APPROVE".equals(action) || "REJECT".equals(action) || "SEND_BACK".equals(action);
    }

    private ResolvedTransaction resolveTransactionContext(CfgTblAppActivityLog activityLog) throws Exception {
        EntityManager em = getEntityManager();
        try {
            return resolveTransactionContext(activityLog, em);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private ResolvedTransaction resolveTransactionContext(CfgTblAppActivityLog activityLog, EntityManager em)
            throws Exception {
        Map<String, Object> payload = readPayloadMap(activityLog.getTxtPayload());
        Integer applicationId = toInteger(payload.get("applicationId"));
        if (applicationId == null) {
            applicationId = activityLog.getSerEntityId();
        }
        if (applicationId == null || applicationId <= 0) {
            return null;
        }

        CfgTblCustomFormApplication application = em.find(CfgTblCustomFormApplication.class, applicationId);
        if (application == null) {
            return null;
        }

        String historyJson = application.getTxtApprovalHistory();
        if (historyJson == null || historyJson.trim().isEmpty()) {
            return null;
        }

        List<Map<String, Object>> history = objectMapper.readValue(historyJson,
                new TypeReference<List<Map<String, Object>>>() {
                });
        if (history.isEmpty()) {
            return null;
        }

        int historyIndex = resolveHistoryIndex(activityLog, history, payload);
        ResolvedTransaction ctx = new ResolvedTransaction();
        ctx.application = application;
        ctx.history = history;
        ctx.historyIndex = historyIndex;
        return ctx;
    }

    private int resolveHistoryIndex(CfgTblAppActivityLog activityLog, List<Map<String, Object>> history,
            Map<String, Object> payload) {
        Integer historyIndex = toInteger(payload.get("historyIndex"));
        if (historyIndex != null && historyIndex >= 0 && historyIndex < history.size()) {
            return historyIndex;
        }

        Integer targetLevel = toInteger(payload.get("level"));
        Integer userId = activityLog.getSerUserId();
        String logAction = normalizeLogAction(activityLog.getTxtActionType());

        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> entry = history.get(i);
            boolean levelMatch = targetLevel == null || targetLevel.equals(toInteger(entry.get("level")));
            boolean userMatch = userId == null || userId.equals(toInteger(entry.get("approvedBy")));
            boolean actionMatch = logAction == null
                    || historyActionMatches(logAction, stringValue(entry.get("action")));
            if (levelMatch && userMatch && actionMatch) {
                return i;
            }
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            Map<String, Object> entry = history.get(i);
            if (userId != null && userId.equals(toInteger(entry.get("approvedBy")))) {
                return i;
            }
        }

        return history.size() - 1;
    }

    private void syncPriorApprovalsEntry(CfgTblCustomFormApplication application, Integer originalApprovedBy,
            Integer originalLevel, String originalAction, String originalApprovedDate, Map<String, Object> entry) {
        try {
            String priorJson = application.getTxtPriorApprovals();
            if (priorJson == null || priorJson.trim().isEmpty()) {
                return;
            }
            List<Map<String, Object>> prior = objectMapper.readValue(priorJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            int matchIndex = findPriorApprovalIndex(prior, originalApprovedBy, originalLevel, originalAction,
                    originalApprovedDate);
            if (matchIndex >= 0) {
                prior.set(matchIndex, new HashMap<>(entry));
                application.setTxtPriorApprovals(objectMapper.writeValueAsString(prior));
            }
        } catch (Exception e) {
            log.warn("syncPriorApprovalsEntry skipped: {}", e.getMessage());
        }
    }

    private int findPriorApprovalIndex(List<Map<String, Object>> prior, Integer originalApprovedBy,
            Integer originalLevel, String originalAction, String originalApprovedDate) {
        if (prior == null || prior.isEmpty()) {
            return -1;
        }
        for (int i = prior.size() - 1; i >= 0; i--) {
            if (priorEntryMatches(prior.get(i), originalApprovedBy, originalLevel, originalAction,
                    originalApprovedDate, true)) {
                return i;
            }
        }
        for (int i = prior.size() - 1; i >= 0; i--) {
            if (priorEntryMatches(prior.get(i), originalApprovedBy, originalLevel, originalAction,
                    originalApprovedDate, false)) {
                return i;
            }
        }
        return -1;
    }

    private boolean priorEntryMatches(Map<String, Object> priorEntry, Integer originalApprovedBy,
            Integer originalLevel, String originalAction, String originalApprovedDate, boolean strictDate) {
        if (priorEntry == null) {
            return false;
        }
        if (originalApprovedBy != null) {
            Integer priorUserId = toInteger(priorEntry.get("approvedBy"));
            if (priorUserId == null || !originalApprovedBy.equals(priorUserId)) {
                return false;
            }
        }
        if (originalLevel != null) {
            Integer priorLevel = toInteger(priorEntry.get("level"));
            if (priorLevel == null) {
                priorLevel = toInteger(priorEntry.get("intApprovalOrder"));
            }
            if (priorLevel == null || !originalLevel.equals(priorLevel)) {
                return false;
            }
        }
        if (originalAction != null && !originalAction.trim().isEmpty()) {
            String priorAction = stringValue(priorEntry.get("action"));
            if (priorAction.isEmpty()) {
                priorAction = stringValue(priorEntry.get("status"));
            }
            if (!priorAction.isEmpty()
                    && !priorAction.trim().equalsIgnoreCase(originalAction.trim())) {
                return false;
            }
        }
        if (strictDate && originalApprovedDate != null && !originalApprovedDate.trim().isEmpty()) {
            String priorDate = firstNonBlank(stringValue(priorEntry.get("approvedDate")),
                    stringValue(priorEntry.get("approvedAt")), stringValue(priorEntry.get("sentBackDate")));
            if (!priorDate.isEmpty() && !priorDate.equals(originalApprovedDate)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Workflow and output preview read assigned approvers from txtApplicationData (footer fields,
     * reviewers, approver, etc.) while prior approvals read txtApprovalHistory / txtPriorApprovals.
     * Keep both in sync when an approval transaction is edited.
     */
    private void syncApplicationFormDataForApproverChange(CfgTblCustomFormApplication application,
            int originalUserId, int newUserId, Map<String, Object> entry, EntityManager em) throws Exception {
        String appDataJson = application.getTxtApplicationData();
        if (appDataJson == null || appDataJson.trim().isEmpty()) {
            return;
        }

        CfgTblUser newUser = commonService.getCurrentUser(newUserId);
        if (newUser == null) {
            return;
        }

        Map<String, Object> appData = objectMapper.readValue(appDataJson,
                new TypeReference<Map<String, Object>>() {
                });
        Map<String, Object> newUserObj = buildFormUserObject(newUser);
        Integer historyLevel = toInteger(entry.get("level"));
        String role = stringValue(entry.get("role"));

        boolean changed = false;
        changed |= replaceUserInApprovalSequence(appData, historyLevel, originalUserId, newUserObj, role);
        changed |= replaceUserInFooterFieldLists(appData, "footerFields", historyLevel, originalUserId, newUserObj,
                role);
        changed |= replaceUserInFooterFieldLists(appData, "individual_pipeline_footer", historyLevel, originalUserId,
                newUserObj, role);
        changed |= replaceUserInFooterFieldLists(appData, "field_footer", historyLevel, originalUserId, newUserObj,
                role);

        if (changed) {
            application.setTxtApplicationData(objectMapper.writeValueAsString(appData));
        }
    }

    private Map<String, Object> buildFormUserObject(CfgTblUser user) {
        Map<String, Object> userObj = new HashMap<>();
        userObj.put("serUserId", user.getSerUserId());
        userObj.put("userId", user.getSerUserId());
        userObj.put("id", user.getSerUserId());
        userObj.put("txtUserName", user.getTxtUserName());
        userObj.put("userName", user.getTxtUserName());
        userObj.put("name", user.getTxtUserName());
        if (user.getTxtDesignation() != null) {
            userObj.put("designation", user.getTxtDesignation());
            userObj.put("txtDesignation", user.getTxtDesignation());
        }
        if (user.getTxtDepartmentName() != null) {
            userObj.put("departmentName", user.getTxtDepartmentName());
            userObj.put("txtDepartmentName", user.getTxtDepartmentName());
        }
        return userObj;
    }

    @SuppressWarnings("unchecked")
    private boolean replaceUserInApprovalSequence(Map<String, Object> appData, Integer historyLevel,
            int originalUserId, Map<String, Object> newUserObj, String role) {
        if (appData == null) {
            return false;
        }

        List<Map<String, Object>> footerFields = extractFooterFields(appData);
        if (!footerFields.isEmpty()) {
            int seqIndex = 0;
            boolean changed = false;
            for (Map<String, Object> field : footerFields) {
                if (field == null) {
                    continue;
                }
                String key = field.get("key") != null ? String.valueOf(field.get("key")).toLowerCase() : "";
                if ("prepared_by".equals(key)) {
                    continue;
                }
                List<Object> users = extractFooterUsers(field);
                for (int i = 0; i < users.size(); i++) {
                    seqIndex++;
                    if (!shouldReplaceAtSequencePosition(seqIndex, historyLevel, field, role)) {
                        continue;
                    }
                    Integer uid = extractUserId(users.get(i));
                    if (uid != null && uid == originalUserId) {
                        users.set(i, new HashMap<>(newUserObj));
                        changed = true;
                    }
                }
            }
            return changed;
        }

        boolean changed = false;
        int seqIndex = 0;

        Object reviewersObj = appData.get("reviewers");
        if (reviewersObj instanceof List) {
            List<Object> reviewers = (List<Object>) reviewersObj;
            for (int i = 0; i < reviewers.size(); i++) {
                seqIndex++;
                if (shouldReplaceAtSequencePosition(seqIndex, historyLevel, null, role)) {
                    Integer uid = extractUserId(reviewers.get(i));
                    if (uid != null && uid == originalUserId) {
                        reviewers.set(i, new HashMap<>(newUserObj));
                        changed = true;
                    }
                }
            }
        }

        Object recommendersObj = appData.get("recommenders");
        if (recommendersObj instanceof List) {
            List<Object> recommenders = (List<Object>) recommendersObj;
            for (int i = 0; i < recommenders.size(); i++) {
                seqIndex++;
                if (shouldReplaceAtSequencePosition(seqIndex, historyLevel, null, role)) {
                    Integer uid = extractUserId(recommenders.get(i));
                    if (uid != null && uid == originalUserId) {
                        recommenders.set(i, new HashMap<>(newUserObj));
                        changed = true;
                    }
                }
            }
        }

        Object approverObj = appData.get("approver");
        if (approverObj != null) {
            seqIndex++;
            if (shouldReplaceAtSequencePosition(seqIndex, historyLevel, null, role)) {
                Integer uid = extractUserId(approverObj);
                if (uid != null && uid == originalUserId) {
                    appData.put("approver", new HashMap<>(newUserObj));
                    changed = true;
                }
            }
        }

        Object approversObj = appData.get("approvers");
        if (approversObj instanceof List) {
            List<Object> approvers = (List<Object>) approversObj;
            for (int i = 0; i < approvers.size(); i++) {
                seqIndex++;
                if (shouldReplaceAtSequencePosition(seqIndex, historyLevel, null, role)) {
                    Integer uid = extractUserId(approvers.get(i));
                    if (uid != null && uid == originalUserId) {
                        approvers.set(i, new HashMap<>(newUserObj));
                        changed = true;
                    }
                }
            }
        }

        return changed;
    }

    @SuppressWarnings("unchecked")
    private boolean replaceUserInFooterFieldLists(Map<String, Object> appData, String key, Integer historyLevel,
            int originalUserId, Map<String, Object> newUserObj, String role) {
        Object obj = appData.get(key);
        if (!(obj instanceof List)) {
            return false;
        }
        List<Map<String, Object>> fields = (List<Map<String, Object>>) obj;
        boolean changed = false;
        int seqIndex = 0;
        for (Map<String, Object> field : fields) {
            if (field == null) {
                continue;
            }
            String fieldKey = field.get("key") != null ? String.valueOf(field.get("key")).toLowerCase() : "";
            if ("prepared_by".equals(fieldKey)) {
                continue;
            }
            List<Object> users = extractFooterUsers(field);
            for (int i = 0; i < users.size(); i++) {
                seqIndex++;
                if (!shouldReplaceAtSequencePosition(seqIndex, historyLevel, field, role)) {
                    continue;
                }
                Integer uid = extractUserId(users.get(i));
                if (uid != null && uid == originalUserId) {
                    users.set(i, new HashMap<>(newUserObj));
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean shouldReplaceAtSequencePosition(int seqIndex, Integer historyLevel, Map<String, Object> field,
            String role) {
        if (historyLevel != null && historyLevel > 0) {
            return seqIndex == historyLevel;
        }
        if (role != null && !role.trim().isEmpty() && field != null) {
            String label = field.get("label") != null ? String.valueOf(field.get("label")) : "";
            if (!label.isEmpty() && label.equalsIgnoreCase(role.trim())) {
                return true;
            }
            String fieldKey = field.get("key") != null ? String.valueOf(field.get("key")) : "";
            if (!fieldKey.isEmpty() && fieldKey.equalsIgnoreCase(role.trim())) {
                return true;
            }
        }
        return historyLevel == null && (role == null || role.trim().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFooterFields(Map<String, Object> appData) {
        if (appData == null) {
            return new ArrayList<>();
        }
        Object obj = appData.get("footerFields");
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        obj = appData.get("individual_pipeline_footer");
        if (obj instanceof List) {
            return (List<Map<String, Object>>) obj;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> extractFooterUsers(Map<String, Object> field) {
        if (field == null) {
            return new ArrayList<>();
        }
        Object usersObj = field.get("users");
        if (usersObj instanceof List) {
            return (List<Object>) usersObj;
        }
        Object selectedUsersObj = field.get("selectedUsers");
        if (selectedUsersObj instanceof List) {
            return (List<Object>) selectedUsersObj;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    private Integer extractUserId(Object userObj) {
        if (userObj == null) {
            return null;
        }
        if (userObj instanceof Number) {
            return ((Number) userObj).intValue();
        }
        if (userObj instanceof Map) {
            Map<String, Object> userMap = (Map<String, Object>) userObj;
            Integer id = toInteger(userMap.get("serUserId"));
            if (id != null) {
                return id;
            }
            id = toInteger(userMap.get("userId"));
            if (id != null) {
                return id;
            }
            return toInteger(userMap.get("id"));
        }
        return toInteger(userObj);
    }

    private Map<String, Object> readPayloadMap(String payloadJson) throws Exception {
        if (payloadJson == null || payloadJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        return objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {
        });
    }

    private String normalizeLogAction(String actionType) {
        if (actionType == null) {
            return null;
        }
        return actionType.trim().toUpperCase();
    }

    private boolean historyActionMatches(String logAction, String historyAction) {
        if (historyAction == null || historyAction.trim().isEmpty()) {
            return true;
        }
        String history = historyAction.trim().toUpperCase();
        if ("APPROVE".equals(logAction)) {
            return history.contains("APPROV");
        }
        if ("REJECT".equals(logAction)) {
            return history.contains("REJECT");
        }
        if ("SEND_BACK".equals(logAction)) {
            return history.contains("SENT_BACK") || history.contains("SEND_BACK");
        }
        return true;
    }

    private String mapHistoryActionToLogAction(String historyAction) {
        if (historyAction == null) {
            return "UPDATE";
        }
        String action = historyAction.trim().toUpperCase();
        if (action.contains("REJECT")) {
            return "REJECT";
        }
        if (action.contains("SENT_BACK") || action.contains("SEND_BACK")) {
            return "SEND_BACK";
        }
        if (action.contains("APPROV")) {
            return "APPROVE";
        }
        return "UPDATE";
    }

    private String buildTransactionMessage(String historyAction) {
        if (historyAction == null || historyAction.trim().isEmpty()) {
            return "Application transaction updated";
        }
        String action = historyAction.trim().toUpperCase();
        if (action.contains("REJECT")) {
            return "Application rejected";
        }
        if (action.contains("SENT_BACK") || action.contains("SEND_BACK")) {
            return "Application sent back";
        }
        if (action.contains("APPROV")) {
            return "Application approved";
        }
        return "Application transaction updated";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static class ResolvedTransaction {
        private CfgTblCustomFormApplication application;
        private List<Map<String, Object>> history;
        private int historyIndex;
    }

    /**
     * Direct DB update of approval history entry — does not invoke workflow pipeline.
     */
    private String syncPayloadToApplicationHistory(EntityManager em, CfgTblAppActivityLog activityLog) {
        try {
            if (activityLog.getTxtPayload() == null || activityLog.getTxtPayload().trim().isEmpty()) {
                return "Failure: No payload to sync";
            }

            Map<String, Object> payload = objectMapper.readValue(activityLog.getTxtPayload(),
                    new TypeReference<Map<String, Object>>() {
                    });

            String targetType = payload.get("targetType") != null ? String.valueOf(payload.get("targetType")) : "";
            if (!"APPROVAL_HISTORY".equalsIgnoreCase(targetType)) {
                return null;
            }

            Integer applicationId = toInteger(payload.get("applicationId"));
            if (applicationId == null) {
                applicationId = activityLog.getSerEntityId();
            }
            if (applicationId == null || applicationId <= 0) {
                return "Failure: Application ID missing in payload";
            }

            CfgTblCustomFormApplication application = em.find(CfgTblCustomFormApplication.class, applicationId);
            if (application == null) {
                return "Failure: Application not found";
            }

            String historyJson = application.getTxtApprovalHistory();
            if (historyJson == null || historyJson.trim().isEmpty()) {
                return "Failure: Application has no approval history";
            }

            List<Map<String, Object>> history = objectMapper.readValue(historyJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            Integer targetLevel = toInteger(payload.get("level"));
            Integer historyIndex = toInteger(payload.get("historyIndex"));
            Map<String, Object> targetEntry = null;

            if (historyIndex != null && historyIndex >= 0 && historyIndex < history.size()) {
                targetEntry = history.get(historyIndex);
            } else if (targetLevel != null) {
                for (Map<String, Object> entry : history) {
                    Integer entryLevel = toInteger(entry.get("level"));
                    if (targetLevel.equals(entryLevel)) {
                        if (activityLog.getSerUserId() != null) {
                            Integer approvedBy = toInteger(entry.get("approvedBy"));
                            if (approvedBy != null && !approvedBy.equals(activityLog.getSerUserId())) {
                                continue;
                            }
                        }
                        targetEntry = entry;
                        break;
                    }
                }
            }

            if (targetEntry == null) {
                return "Failure: Matching approval history entry not found";
            }

            if (payload.containsKey("remarks")) {
                targetEntry.put("remarks", payload.get("remarks"));
            }
            if (payload.containsKey("role")) {
                targetEntry.put("role", payload.get("role"));
            }
            if (payload.containsKey("action")) {
                targetEntry.put("action", payload.get("action"));
            }
            if (payload.containsKey("approvedIp") && payload.get("approvedIp") != null) {
                targetEntry.put("approvedIp", payload.get("approvedIp"));
            }
            if (payload.containsKey("approvedVia") && payload.get("approvedVia") != null) {
                targetEntry.put("approvedVia", payload.get("approvedVia"));
            }

            application.setTxtApprovalHistory(objectMapper.writeValueAsString(history));
            if (payload.containsKey("remarks")) {
                application.setTxtRemarks(String.valueOf(payload.get("remarks")));
            }
            em.merge(application);
            return null;
        } catch (Exception e) {
            log.error("syncPayloadToApplicationHistory failed: {}", e.getMessage(), e);
            return "Failure: " + e.getMessage();
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
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Timestamp parseDateStart(String date) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return new Timestamp(sdf.parse(date).getTime());
    }

    private Timestamp parseDateEnd(String date) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return new Timestamp(sdf.parse(date + " 23:59:59").getTime());
    }
}
