package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailTemplateRepository extends JpaRepository<CfgTblEmailTemplate, Integer> {

    List<CfgTblEmailTemplate> findByBlIsDeletedFalseOrderByIntSortOrderAscTxtTemplateNameAsc();

    List<CfgTblEmailTemplate> findByBlIsDeletedFalseAndTxtCategoryIgnoreCaseOrderByIntSortOrderAscTxtTemplateNameAsc(
            String category);

    Optional<CfgTblEmailTemplate> findBySerEmailTemplateIdAndBlIsDeletedFalse(Integer id);

    Optional<CfgTblEmailTemplate> findByTxtTemplateCodeIgnoreCaseAndBlIsDeletedFalse(String code);

    boolean existsByTxtTemplateCodeIgnoreCaseAndBlIsDeletedFalseAndSerEmailTemplateIdNot(String code, Integer id);

    long countBySerEmailLayoutIdAndBlIsDeletedFalse(Integer layoutId);
}
