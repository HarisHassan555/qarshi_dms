package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomFormApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Persists template workflow state and queues post-approval emails in one Spring transaction
 * (same pattern as delegation notify emails).
 */
@Service
public class TemplateWorkflowCommandService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TemplateEmailDispatchService templateEmailDispatchService;

    @Transactional
    public void mergeAndQueuePostApprovalEmails(CfgTblCustomFormApplication application, String contextLabel,
            boolean refreshPdfBeforeEmail) {
        if (application == null || application.getSerApplicationId() == null) {
            return;
        }
        entityManager.merge(application);
        templateEmailDispatchService.queuePostApprovalEmails(application.getSerApplicationId(), contextLabel,
                refreshPdfBeforeEmail);
    }
}
