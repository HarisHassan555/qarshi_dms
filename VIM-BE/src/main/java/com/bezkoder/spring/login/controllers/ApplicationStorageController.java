package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.ApplicationStorageInfoService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class ApplicationStorageController {

    private static final Logger logger = LogManager.getLogger(ApplicationStorageController.class);

    @Autowired
    private ApplicationStorageInfoService applicationStorageInfoService;

    @Autowired
    private ICommonService commonService;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/getApplicationStorageInfo")
    public Map<String, Object> getApplicationStorageInfo(HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        try {
            int userId = commonService.getCurrentLoggedInUser();
            if (!isAdminUser(userId)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                result.put("status", "Failure");
                result.put("message", "Only administrators can view storage information.");
                return result;
            }
            return applicationStorageInfoService.buildStorageInfo();
        } catch (Exception ex) {
            logger.error("Failed to load application storage info: " + ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("status", "Failure");
            result.put("message", ex.getMessage());
            return result;
        }
    }

    private boolean isAdminUser(int userId) {
        if (userId <= 0) {
            return false;
        }
        CfgTblUser user = entityManager.find(CfgTblUser.class, userId);
        if (user == null) {
            return false;
        }
        CfgTblRole role = user.getCfgTblRole();
        String roleName = role != null ? role.getTxtRoleName() : null;
        if (roleName == null || roleName.trim().isEmpty()) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("ADMIN")
                || normalized.equals("ROLE_ADMIN")
                || normalized.equals("SUPER ADMIN")
                || normalized.equals("ROLE_SUPER ADMIN")
                || normalized.contains("ADMIN");
    }
}
