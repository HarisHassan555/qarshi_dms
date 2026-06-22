package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.bll.services.IAppActivityLogService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblAppActivityLogDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblAppActivityLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AppActivityLogService implements IAppActivityLogService {

    private final Logger logger = LogManager.getLogger(AppActivityLogService.class);

    @Autowired
    private ICfgTblAppActivityLogDAO activityLogDAO;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<CfgTblAppActivityLog> getAllActivityLogs() {
        return activityLogDAO.getAllActivityLogs();
    }

    @Override
    public List<CfgTblAppActivityLog> getActivityLogsByFilters(String actionType, String status, Integer userId,
            Integer entityId, String startDate, String endDate) {
        return activityLogDAO.getActivityLogsByFilters(actionType, status, userId, entityId, startDate, endDate);
    }

    @Override
    public CfgTblAppActivityLog getActivityLogById(Integer id) {
        return activityLogDAO.getActivityLogById(id);
    }

    @Override
    public String updateActivityLog(CfgTblAppActivityLog log, boolean syncToSource) {
        return activityLogDAO.updateActivityLog(log, syncToSource);
    }

    @Override
    public Map<String, Object> getTransactionForActivityLog(Integer logId) {
        return activityLogDAO.getTransactionForActivityLog(logId);
    }

    @Override
    public String updateTransactionForActivityLog(Integer logId, Map<String, Object> updates) {
        return activityLogDAO.updateTransactionForActivityLog(logId, updates);
    }

    @Override
    public void logActivity(String actionType, Integer userId, String username, String ip, String device,
            String status, String message, String entityType, Integer entityId, Map<String, Object> payload,
            String errorMessage) {
        try {
            CfgTblAppActivityLog log = new CfgTblAppActivityLog();
            log.setTxtActionType(actionType != null ? actionType.toUpperCase() : "UNKNOWN");
            log.setSerUserId(userId);
            log.setTxtUsername(username);
            log.setTxtIpAddress(ip);
            log.setTxtDevice(device);
            log.setTxtStatus(status != null ? status.toUpperCase() : "SUCCESS");
            log.setTxtMessage(message);
            log.setTxtEntityType(entityType);
            log.setSerEntityId(entityId);
            log.setTxtErrorMessage(errorMessage);
            if (payload != null && !payload.isEmpty()) {
                log.setTxtPayload(objectMapper.writeValueAsString(payload));
            }
            activityLogDAO.addActivityLog(log);
        } catch (Exception e) {
            logger.error("Failed to write activity log: " + e.getMessage(), e);
        }
    }
}
