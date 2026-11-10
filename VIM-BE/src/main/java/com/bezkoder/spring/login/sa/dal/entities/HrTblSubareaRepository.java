package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrTblSubareaRepository extends JpaRepository<HrTblSubarea, Integer> {

    List<HrTblSubarea> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtSubareaNameAsc();

    @Query("SELECT s FROM HrTblSubarea s WHERE s.serOrganizationId = :orgId "
            + "AND (s.blIsDeleted IS NULL OR s.blIsDeleted = false) AND ("
            + "LOWER(TRIM(s.txtSubareaName)) = LOWER(TRIM(:name)) "
            + "OR LOWER(TRIM(COALESCE(s.txtSubareaCode, ''))) = LOWER(TRIM(:code)))")
    List<HrTblSubarea> findActiveByOrganizationAndNameOrCode(
            @Param("orgId") Integer orgId, @Param("name") String name, @Param("code") String code);
}
