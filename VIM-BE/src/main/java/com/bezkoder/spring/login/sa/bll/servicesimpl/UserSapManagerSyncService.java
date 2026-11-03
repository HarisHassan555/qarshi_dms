package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblSapHcmEmployee;
import com.bezkoder.spring.login.sa.dal.entities.SapHcmEmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class UserSapManagerSyncService {

    private static final Logger log = LoggerFactory.getLogger(UserSapManagerSyncService.class);

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private SapHcmEmployeeRepository sapHcmEmployeeRepository;

    public Map<String, Object> resolveManagerFromSapEmployeeId(String employeeId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "Success");
        String normalizedEmployeeId = normalizeEmployeeCode(employeeId);
        if (normalizedEmployeeId == null) {
            result.put("message", "Employee ID is required");
            return result;
        }

        Optional<CfgTblSapHcmEmployee> sapRow = sapHcmEmployeeRepository
                .findByTxtEmployeeIdAndBlIsDeletedFalse(normalizedEmployeeId);
        if (!sapRow.isPresent()) {
            result.put("message", "No SAP HCM row for this employee ID");
            return result;
        }

        CfgTblSapHcmEmployee employee = sapRow.get();
        String reportTo = normalizeReportToCode(employee.getTxtReportTo());
        result.put("reportTo", reportTo);
        result.put("reportToName", trimToNull(employee.getTxtReportToName()));

        if (reportTo == null) {
            result.put("message", "SAP row has no valid report-to manager code");
            return result;
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            CfgTblUser managerUser = findUserByEmployeeCode(entityManager, reportTo);
            if (managerUser == null) {
                result.put("message", "No DMS user found with Employee ID matching SAP report-to code " + reportTo);
                return result;
            }
            result.put("managerUserId", managerUser.getSerUserId());
            result.put("managerUserName", managerUser.getTxtUserName());
            result.put("managerEmployeeId", trimToNull(managerUser.getTxtCnic()));
            result.put("message", "Manager resolved from SAP");
            return result;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    public Map<String, Object> syncAllUserManagersFromSap() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", "Success");
        int examined = 0;
        int updated = 0;
        int cleared = 0;
        int skippedNoSap = 0;
        int skippedNoReportTo = 0;
        int skippedNoManagerUser = 0;
        int skippedSelf = 0;

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            @SuppressWarnings("unchecked")
            List<CfgTblUser> users = entityManager.createQuery(
                    "SELECT u FROM CfgTblUser u WHERE (u.blIsDeleted IS NULL OR u.blIsDeleted = FALSE)",
                    CfgTblUser.class).getResultList();

            for (CfgTblUser user : users) {
                if (user == null || user.getSerUserId() == null) {
                    continue;
                }
                String employeeCode = normalizeEmployeeCode(user.getTxtCnic());
                if (employeeCode == null) {
                    continue;
                }
                examined++;

                Optional<CfgTblSapHcmEmployee> sapRow = sapHcmEmployeeRepository
                        .findByTxtEmployeeIdAndBlIsDeletedFalse(employeeCode);
                if (!sapRow.isPresent()) {
                    skippedNoSap++;
                    continue;
                }

                String reportTo = normalizeReportToCode(sapRow.get().getTxtReportTo());
                if (reportTo == null) {
                    if (user.getCfgTblManager() != null) {
                        user.setCfgTblManager(null);
                        entityManager.merge(user);
                        cleared++;
                    }
                    skippedNoReportTo++;
                    continue;
                }

                if (employeeCode.equals(reportTo)) {
                    skippedSelf++;
                    continue;
                }

                CfgTblUser managerUser = findUserByEmployeeCode(entityManager, reportTo);
                if (managerUser == null) {
                    skippedNoManagerUser++;
                    continue;
                }

                if (user.getSerUserId().equals(managerUser.getSerUserId())) {
                    skippedSelf++;
                    continue;
                }

                Integer currentManagerId = user.getCfgTblManager() != null ? user.getCfgTblManager().getSerUserId() : null;
                if (currentManagerId != null && currentManagerId.equals(managerUser.getSerUserId())) {
                    continue;
                }

                user.setCfgTblManager(managerUser);
                entityManager.merge(user);
                updated++;
            }

            entityManager.getTransaction().commit();
        } catch (Exception ex) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            log.error("syncAllUserManagersFromSap failed: {}", ex.getMessage(), ex);
            summary.put("status", "Failure");
            summary.put("message", ex.getMessage());
            return summary;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }

        summary.put("message", "Manager sync completed");
        summary.put("examined", examined);
        summary.put("updated", updated);
        summary.put("cleared", cleared);
        summary.put("skippedNoSap", skippedNoSap);
        summary.put("skippedNoReportTo", skippedNoReportTo);
        summary.put("skippedNoManagerUser", skippedNoManagerUser);
        summary.put("skippedSelf", skippedSelf);
        return summary;
    }

    /**
     * Validates SAP-sourced user creation: when SAP report-to is set, the manager must already exist in Users.
     * On success, sets cfgTblManager on the user from report-to employee code.
     *
     * @return null if OK to create; otherwise a user-facing error message
     */
    public String prepareManagerForSapUserCreate(CfgTblUser user) {
        if (user == null || !Boolean.TRUE.equals(user.getCreatedFromSapHcm())) {
            return null;
        }
        if (isManagerExemptRole(user)) {
            return null;
        }

        String employeeCode = normalizeEmployeeCode(user.getTxtCnic());
        if (employeeCode == null) {
            return null;
        }

        Optional<CfgTblSapHcmEmployee> sapRow = sapHcmEmployeeRepository
                .findByTxtEmployeeIdAndBlIsDeletedFalse(employeeCode);
        if (!sapRow.isPresent()) {
            return null;
        }

        String reportTo = normalizeReportToCode(sapRow.get().getTxtReportTo());
        if (reportTo == null) {
            return null;
        }

        if (employeeCode.equals(reportTo)) {
            return null;
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            CfgTblUser managerUser = findUserByEmployeeCode(entityManager, reportTo);
            if (managerUser == null) {
                String managerLabel = trimToNull(sapRow.get().getTxtReportToName());
                if (managerLabel != null) {
                    return "Cannot create this user: manager \"" + managerLabel + "\" (Employee ID "
                            + reportTo + ") is not in Users yet. Create the manager user first, then create this user.";
                }
                return "Cannot create this user: manager with Employee ID " + reportTo
                        + " is not in Users yet. Create the manager user first, then create this user.";
            }
            user.setCfgTblManager(managerUser);
            return null;
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private boolean isManagerExemptRole(CfgTblUser user) {
        String roleName = user.getTxtrole();
        if (roleName == null && user.getCfgTblRole() != null) {
            roleName = user.getCfgTblRole().getTxtRoleName();
        }
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("vendor") || normalized.equals("admin") || normalized.contains("administrator");
    }

    private CfgTblUser findUserByEmployeeCode(EntityManager entityManager, String employeeCode) {
        if (entityManager == null || employeeCode == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<CfgTblUser> matches = entityManager.createQuery(
                "SELECT u FROM CfgTblUser u WHERE TRIM(u.txtCnic) = :code "
                        + "AND (u.blIsDeleted IS NULL OR u.blIsDeleted = FALSE)",
                CfgTblUser.class)
                .setParameter("code", employeeCode)
                .setMaxResults(1)
                .getResultList();
        return matches.isEmpty() ? null : matches.get(0);
    }

    static String normalizeEmployeeCode(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeReportToCode(String raw) {
        String code = normalizeEmployeeCode(raw);
        if (code == null) {
            return null;
        }
        String upper = code.toUpperCase(Locale.ROOT);
        if ("0".equals(code) || "00000000".equals(upper)) {
            return null;
        }
        return code;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
