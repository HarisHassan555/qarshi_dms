package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowPipelineRepository extends JpaRepository<CfgTblWorkflowPipeline, Integer> {

    List<CfgTblWorkflowPipeline> findByBlIsDeletedFalseOrderByDteModifiedDateDescTxtNameAsc();

    Optional<CfgTblWorkflowPipeline> findBySerWorkflowPipelineIdAndBlIsDeletedFalse(Integer id);

    Optional<CfgTblWorkflowPipeline> findByTxtCodeIgnoreCaseAndBlIsDeletedFalse(String code);

    boolean existsByTxtCodeIgnoreCaseAndBlIsDeletedFalseAndSerWorkflowPipelineIdNot(String code, Integer excludeId);
}
