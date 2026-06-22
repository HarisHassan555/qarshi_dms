package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblAppActivityLog;

import java.util.List;
import java.util.Map;

public interface IAppActivityLogService {

    List<CfgTblAppActivityLog> getAllActivityLogs();

    List<CfgTblAppActivityLog> getActivityLogsByFilters(String actionType, String status, Integer userId,
            Integer entityId, String startDate, String endDate);

    CfgTblAppActivityLog getActivityLogById(Integer id);

    String updateActivityLog(CfgTblAppActivityLog log, boolean syncToSource);

    Map<String, Object> getTransactionForActivityLog(Integer logId);

    String updateTransactionForActivityLog(Integer logId, Map<String, Object> updates);

    void logActivity(String actionType, Integer userId, String username, String ip, String device,
            String status, String message, String entityType, Integer entityId, Map<String, Object> payload,
            String errorMessage);
}
