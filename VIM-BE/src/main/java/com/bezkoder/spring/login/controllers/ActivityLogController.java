package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblAppActivityLog;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class ActivityLogController {

    private final Logger logger = LogManager.getLogger(ActivityLogController.class);

    @Autowired
    private IAppActivityLogService activityLogService;

    @Autowired
    private IMenuService menuService;

    @Autowired
    private ICommonService commonService;

    private static final String ACTIVITY_LOGS_SUBMENU_URL = "activitylogs";

    @RequestMapping(value = "/getAllActivityLogs", method = RequestMethod.GET)
    public List<CfgTblAppActivityLog> getAllActivityLogs(
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) Integer entityId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getAllActivityLogs()");
        try {
            if (!hasActivityLogsViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return Collections.emptyList();
            }

            boolean hasFilters = (actionType != null && !actionType.trim().isEmpty())
                    || (status != null && !status.trim().isEmpty())
                    || (userId != null && userId > 0)
                    || (entityId != null && entityId > 0)
                    || (startDate != null && !startDate.trim().isEmpty())
                    || (endDate != null && !endDate.trim().isEmpty());

            if (hasFilters) {
                return activityLogService.getActivityLogsByFilters(actionType, status, userId, entityId, startDate,
                        endDate);
            }
            return activityLogService.getAllActivityLogs();
        } catch (Exception ex) {
            logger.error("Error fetching activity logs: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/getActivityLogById", method = RequestMethod.GET)
    public CfgTblAppActivityLog getActivityLogById(@RequestParam Integer id,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getActivityLogById() - id: " + id);
        try {
            if (!hasActivityLogsViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            return activityLogService.getActivityLogById(id);
        } catch (Exception ex) {
            logger.error("Error fetching activity log: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    @RequestMapping(value = "/updateActivityLog", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateActivityLog(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("updateActivityLog()");
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasActivityLogsUpdatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to edit activity logs");
                return result;
            }

            CfgTblAppActivityLog log = new CfgTblAppActivityLog();
            if (requestBody.get("serActivityLogId") != null) {
                log.setSerActivityLogId((Integer) requestBody.get("serActivityLogId"));
            }
            if (requestBody.get("txtActionType") != null) {
                log.setTxtActionType(String.valueOf(requestBody.get("txtActionType")));
            }
            if (requestBody.get("serUserId") != null) {
                log.setSerUserId((Integer) requestBody.get("serUserId"));
            }
            if (requestBody.get("txtUsername") != null) {
                log.setTxtUsername(String.valueOf(requestBody.get("txtUsername")));
            }
            if (requestBody.get("txtIpAddress") != null) {
                log.setTxtIpAddress(String.valueOf(requestBody.get("txtIpAddress")));
            }
            if (requestBody.get("txtDevice") != null) {
                log.setTxtDevice(String.valueOf(requestBody.get("txtDevice")));
            }
            if (requestBody.get("txtStatus") != null) {
                log.setTxtStatus(String.valueOf(requestBody.get("txtStatus")));
            }
            if (requestBody.get("txtMessage") != null) {
                log.setTxtMessage(String.valueOf(requestBody.get("txtMessage")));
            }
            if (requestBody.get("serEntityId") != null) {
                log.setSerEntityId((Integer) requestBody.get("serEntityId"));
            }
            if (requestBody.get("txtEntityType") != null) {
                log.setTxtEntityType(String.valueOf(requestBody.get("txtEntityType")));
            }
            if (requestBody.get("txtPayload") != null) {
                log.setTxtPayload(String.valueOf(requestBody.get("txtPayload")));
            }
            if (requestBody.get("txtErrorMessage") != null) {
                log.setTxtErrorMessage(String.valueOf(requestBody.get("txtErrorMessage")));
            }
            if (requestBody.get("dteCreatedDate") != null) {
                log.setDteCreatedDate(java.sql.Timestamp.valueOf(String.valueOf(requestBody.get("dteCreatedDate"))));
            }

            boolean syncToSource = Boolean.TRUE.equals(requestBody.get("syncToSource"));
            String status = activityLogService.updateActivityLog(log, syncToSource);

            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", syncToSource
                        ? "Activity log updated and source data synced"
                        : "Activity log updated");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : status);
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error updating activity log: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
        }
    }

    @RequestMapping(value = "/getActivityLogTransaction", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getActivityLogTransaction(@RequestParam Integer id,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("getActivityLogTransaction() - id: " + id);
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasActivityLogsViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to view activity logs");
                result.put("editable", false);
                return result;
            }
            Map<String, Object> transaction = activityLogService.getTransactionForActivityLog(id);
            transaction.put("status", "Success");
            return transaction;
        } catch (Exception ex) {
            logger.error("Error loading activity log transaction: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            result.put("editable", false);
            return result;
        }
    }

    @RequestMapping(value = "/updateActivityLogTransaction", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateActivityLogTransaction(@RequestBody Map<String, Object> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {
        logger.debug("updateActivityLogTransaction()");
        Map<String, Object> result = new HashMap<>();
        try {
            if (!hasActivityLogsUpdatePermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "You do not have permission to edit activity logs");
                return result;
            }

            Integer logId = toInteger(requestBody.get("serActivityLogId"));
            String status = activityLogService.updateTransactionForActivityLog(logId, requestBody);
            if ("Success".equals(status)) {
                result.put("status", "Success");
                result.put("message", "Application transaction updated (workflow not re-run)");
            } else {
                result.put("status", "Failure");
                result.put("message", status != null && status.startsWith("Failure:") ? status.substring(8) : status);
            }
            return result;
        } catch (Exception ex) {
            logger.error("Error updating activity log transaction: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return result;
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

    private boolean hasActivityLogsViewPermission() {
        return hasActivityLogsPermission(false);
    }

    private boolean hasActivityLogsUpdatePermission() {
        return hasActivityLogsPermission(true);
    }

    private boolean hasActivityLogsPermission(boolean requireUpdate) {
        int userId = commonService.getCurrentLoggedInUser();
        if (userId <= 0) {
            return false;
        }

        CfgTblUser user = commonService.getCurrentUser(userId);
        if (user == null || user.getCfgTblRole() == null || user.getCfgTblRole().getSerRoleId() == null) {
            return false;
        }

        Long roleId = Long.valueOf(user.getCfgTblRole().getSerRoleId());
        List<CfgTblSubMenuRole> permissions = menuService.getAllSubMenuRole(roleId, Long.valueOf(userId));
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (CfgTblSubMenuRole permission : permissions) {
            if (!isPermissionEnabled(permission)) {
                continue;
            }
            CfgTblSubMenu subMenu = permission.getCfgTblSubMenu();
            if (subMenu == null || !matchesActivityLogsUrl(subMenu.getTxtSubMenuUrl())) {
                continue;
            }
            if (requireUpdate) {
                if (isTrue(permission.getBlIsUpdate()) || isTrue(permission.getBlIsNewUpdate())) {
                    return true;
                }
            } else if (isTrue(permission.getBlIsview()) || isTrue(permission.getBlIsNewView())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPermissionEnabled(CfgTblSubMenuRole permission) {
        if (permission == null) {
            return false;
        }
        if (isTrue(permission.getBlIsDeleted())) {
            return false;
        }
        if (!isTrue(permission.getBlIsEnabled())) {
            return false;
        }
        return isTrue(permission.getBlIsActive()) || isTrue(permission.getBlnStatus());
    }

    private boolean matchesActivityLogsUrl(String url) {
        if (url == null) {
            return false;
        }
        String normalized = url.trim().replaceAll("^/+", "").toLowerCase();
        return ACTIVITY_LOGS_SUBMENU_URL.equals(normalized);
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}
