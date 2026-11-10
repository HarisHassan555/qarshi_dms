package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CfgTblOfficialDocumentArchiveRepository extends JpaRepository<CfgTblOfficialDocumentArchive, Integer> {

    @Query("SELECT d FROM CfgTblOfficialDocumentArchive d "
            + "WHERE (d.blIsDeleted = false OR d.blIsDeleted IS NULL) "
            + "AND (:label IS NULL OR :label = '' OR UPPER(d.txtLabel) = UPPER(:label)) "
            + "AND (:q IS NULL OR :q = '' OR "
            + "LOWER(d.txtTitle) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(d.txtSummary) LIKE LOWER(CONCAT('%', :q, '%')) OR "
            + "LOWER(d.txtOriginalFileName) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "ORDER BY d.dteCreatedDate DESC")
    Page<CfgTblOfficialDocumentArchive> search(
            @Param("q") String q,
            @Param("label") String label,
            Pageable pageable);
}
