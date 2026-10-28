package com.bezkoder.spring.login.backgroundjob;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblBackgroundJob;

public interface BackgroundJobHandler {

    String jobType();

    void execute(CfgTblBackgroundJob job) throws Exception;
}
