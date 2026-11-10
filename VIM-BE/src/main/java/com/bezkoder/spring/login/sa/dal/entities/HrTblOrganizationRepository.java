package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrTblOrganizationRepository extends JpaRepository<HrTblOrganization, Integer> {

    List<HrTblOrganization> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtOrganizationNameAsc();

    @Query("SELECT o FROM HrTblOrganization o WHERE LOWER(TRIM(o.txtOrganizationName)) = LOWER(TRIM(:name)) "
            + "AND (o.blIsDeleted IS NULL OR o.blIsDeleted = false)")
    List<HrTblOrganization> findActiveByNameIgnoreCase(@Param("name") String name);

    @Query("SELECT o FROM HrTblOrganization o WHERE (o.blIsDeleted IS NULL OR o.blIsDeleted = false) AND ("
            + "LOWER(TRIM(o.txtOrganizationName)) = LOWER(TRIM(:name)) "
            + "OR LOWER(TRIM(COALESCE(o.txtOrganizationCode, ''))) = LOWER(TRIM(:code)))")
    List<HrTblOrganization> findActiveByNameOrCode(@Param("name") String name, @Param("code") String code);
}
