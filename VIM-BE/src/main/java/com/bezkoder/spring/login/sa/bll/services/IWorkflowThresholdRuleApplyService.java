package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;

public interface IWorkflowThresholdRuleApplyService {

    /**
     * Evaluates active threshold rules for the application's form, mutates
     * {@code templatePayload.pipeline} inside {@code txtApplicationData} when rules match, and
     * records applied rule ids under {@code thresholdRulesApplied}.
     */
    void applyActiveRulesOnSubmit(CfgTblCustomFormApplication application);
}
