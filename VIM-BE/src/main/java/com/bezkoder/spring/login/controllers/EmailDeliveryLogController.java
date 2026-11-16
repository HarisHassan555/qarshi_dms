package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IMenuService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.EmailDeliveryLogDto;
import com.bezkoder.spring.login.sa.bll.servicesimpl.EmailDeliveryLogService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class EmailDeliveryLogController {

    private static final Logger logger = LogManager.getLogger(EmailDeliveryLogController.class);
    private static final String SUBMENU_URL = "email-delivery-log";

    @Autowired
    private EmailDeliveryLogService emailDeliveryLogService;

    @Autowired
    private ICommonService commonService;

    @Autowired
    private IMenuService menuService;

    @RequestMapping(value = "/searchEmailDeliveryLogs", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> searchEmailDeliveryLogs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggerType,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) Integer applicationId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return forbiddenMap();
            }
            return emailDeliveryLogService.search(status, triggerType, recipient, applicationId, page, size);
        } catch (Exception ex) {
            logger.error("searchEmailDeliveryLogs failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return errorMap(ex.getMessage());
        }
    }

    @RequestMapping(value = "/getEmailDeliveryLogById", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public EmailDeliveryLogDto getEmailDeliveryLogById(@RequestParam Long id, HttpServletResponse response) {
        try {
            if (!hasViewPermission()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return null;
            }
            EmailDeliveryLogDto dto = emailDeliveryLogService.getById(id);
            if (dto == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return dto;
        } catch (Exception ex) {
            logger.error("getEmailDeliveryLogById failed: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return null;
        }
    }

    private Map<String, Object> forbiddenMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "Failure");
        result.put("message", "Forbidden");
        result.put("items", Collections.emptyList());
        result.put("total", 0);
        return result;
    }

    private Map<String, Object> errorMap(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "Failure");
        result.put("message", message);
        result.put("items", Collections.emptyList());
        result.put("total", 0);
        return result;
    }

    private enum PermissionMode {
        VIEW
    }

    private boolean hasViewPermission() {
        return hasPermission(PermissionMode.VIEW);
    }

    private boolean hasPermission(PermissionMode mode) {
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
            if (subMenu == null || !matchesSubMenuUrl(subMenu.getTxtSubMenuUrl())) {
                continue;
            }
            if (mode == PermissionMode.VIEW
                    && (isTrue(permission.getBlIsview()) || isTrue(permission.getBlIsNewView()))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPermissionEnabled(CfgTblSubMenuRole permission) {
        if (permission == null || isTrue(permission.getBlIsDeleted())) {
            return false;
        }
        if (!isTrue(permission.getBlIsEnabled())) {
            return false;
        }
        return isTrue(permission.getBlIsActive()) || isTrue(permission.getBlnStatus());
    }

    private boolean matchesSubMenuUrl(String url) {
        if (url == null) {
            return false;
        }
        String normalized = url.trim().replaceAll("^/+", "").toLowerCase();
        if (SUBMENU_URL.equals(normalized)) {
            return true;
        }
        return normalized.endsWith("/" + SUBMENU_URL);
    }

    private boolean isTrue(Boolean value) {
        return Boolean.TRUE.equals(value);
    }
}