package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrTblOrganizationRepository extends JpaRepository<HrTblOrganization, Integer> {

    List<HrTblOrganization> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtOrganizationNameAsc();

    @Query("SELECT o FROM HrTblOrganization o WHERE LOWER(TRIM(o.txtOrganizationName)) = LOWER(TRIM(:name)) "
            + "AND (o.blIsDeleted IS NULL OR o.blIsDeleted = false)")
    Optional<HrTblOrganization> findActiveByNameIgnoreCase(@Param("name") String name);
}
