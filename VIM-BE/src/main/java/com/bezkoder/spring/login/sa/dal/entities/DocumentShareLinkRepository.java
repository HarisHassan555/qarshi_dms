package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentShareLinkRepository extends JpaRepository<CfgTblDocumentShareLink, Integer> {

    Optional<CfgTblDocumentShareLink> findByTxtShareTokenAndBlIsDeletedFalse(String token);

    List<CfgTblDocumentShareLink> findBySerApplicationIdAndBlIsDeletedFalseOrderByDteCreatedDateDesc(
            Integer serApplicationId);
}
