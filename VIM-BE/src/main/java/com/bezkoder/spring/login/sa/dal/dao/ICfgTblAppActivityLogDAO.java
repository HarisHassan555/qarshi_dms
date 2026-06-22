package com.bezkoder.spring.login.sa.dal.dao;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblAppActivityLog;

import java.util.List;
import java.util.Map;

public interface ICfgTblAppActivityLogDAO {

    List<CfgTblAppActivityLog> getAllActivityLogs();

    List<CfgTblAppActivityLog> getActivityLogsByFilters(String actionType, String status, Integer userId,
            Integer entityId, String startDate, String endDate);

    CfgTblAppActivityLog getActivityLogById(Integer id);

    String addActivityLog(CfgTblAppActivityLog log);

    String updateActivityLog(CfgTblAppActivityLog log, boolean syncToSource);

    Map<String, Object> getTransactionForActivityLog(Integer logId);

    String updateTransactionForActivityLog(Integer logId, Map<String, Object> updates);
}
