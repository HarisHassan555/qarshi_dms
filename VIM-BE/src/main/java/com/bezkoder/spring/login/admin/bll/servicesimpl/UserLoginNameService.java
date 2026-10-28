package com.bezkoder.spring.login.admin.bll.servicesimpl;

import com.bezkoder.spring.login.admin.dal.dao.ICfgTblUserDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class UserLoginNameService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    @Autowired
    private ICfgTblUserDAO userDAO;

    public String suggestUniqueLoginName(String employeeId, String fullName) {
        String base = buildBaseLoginName(employeeId, fullName);
        if (base.isEmpty()) {
            base = "user";
        }
        if (!isLoginNameTaken(base)) {
            return base;
        }
        String empSuffix = sanitizeToken(employeeId);
        if (!empSuffix.isEmpty()) {
            String withEmp = trimLength(base + "." + empSuffix, 48);
            if (!isLoginNameTaken(withEmp)) {
                return withEmp;
            }
        }
        for (int i = 0; i < 30; i++) {
            String candidate = trimLength(base + "." + randomSuffix(4), 48);
            if (!isLoginNameTaken(candidate)) {
                return candidate;
            }
        }
        return trimLength(base + "." + randomSuffix(8), 48);
    }

    public boolean isLoginNameTaken(String loginName) {
        if (loginName == null || loginName.trim().isEmpty()) {
            return false;
        }
        CfgTblUser probe = new CfgTblUser();
        probe.setTxtLoginName(loginName.trim());
        java.util.List<CfgTblUser> matches = userDAO.searchUser(probe);
        return matches != null && !matches.isEmpty();
    }

    private String buildBaseLoginName(String employeeId, String fullName) {
        String fromName = slugifyName(fullName);
        String emp = sanitizeToken(employeeId);
        if (!fromName.isEmpty() && !emp.isEmpty()) {
            return trimLength(fromName + "." + emp, 40);
        }
        if (!fromName.isEmpty()) {
            return trimLength(fromName, 40);
        }
        return trimLength(emp, 40);
    }

    private static String slugifyName(String fullName) {
        if (fullName == null) {
            return "";
        }
        String[] parts = fullName.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length == 0) {
            return "";
        }
        if (parts.length == 1) {
            return sanitizeToken(parts[0]);
        }
        return sanitizeToken(parts[0]) + "." + sanitizeToken(parts[parts.length - 1]);
    }

    private static String sanitizeToken(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "").replaceAll("\\.+", ".");
    }

    private static String randomSuffix(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(SUFFIX_CHARS.charAt(RANDOM.nextInt(SUFFIX_CHARS.length())));
        }
        return sb.toString();
    }

    private static String trimLength(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
