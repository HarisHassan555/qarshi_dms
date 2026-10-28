package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrTblSubareaRepository extends JpaRepository<HrTblSubarea, Integer> {

    List<HrTblSubarea> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtSubareaNameAsc();

    @Query("SELECT s FROM HrTblSubarea s WHERE s.serOrganizationId = :orgId "
            + "AND LOWER(TRIM(s.txtSubareaName)) = LOWER(TRIM(:name)) "
            + "AND (s.blIsDeleted IS NULL OR s.blIsDeleted = false)")
    Optional<HrTblSubarea> findActiveByOrganizationAndNameIgnoreCase(
            @Param("orgId") Integer orgId, @Param("name") String name);
}
