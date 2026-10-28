package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowThresholdRuleRepository extends JpaRepository<CfgTblWorkflowThresholdRule, Integer> {

    List<CfgTblWorkflowThresholdRule> findByBlIsDeletedFalseOrderByIntPriorityAscSerFormIdAscTxtRuleNameAsc();

    List<CfgTblWorkflowThresholdRule> findBySerFormIdAndBlIsDeletedFalseOrderByIntPriorityAscTxtRuleNameAsc(Integer serFormId);

    Optional<CfgTblWorkflowThresholdRule> findBySerWorkflowThresholdRuleIdAndBlIsDeletedFalse(Integer id);
}
