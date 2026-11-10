package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrTblDivisionRepository extends JpaRepository<HrTblDivision, Integer> {

    List<HrTblDivision> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtDivisionNameAsc();

    @Query("SELECT d FROM HrTblDivision d WHERE d.serSubareaId = :subareaId "
            + "AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false) AND ("
            + "LOWER(TRIM(d.txtDivisionName)) = LOWER(TRIM(:name)) "
            + "OR LOWER(TRIM(COALESCE(d.txtDivisionCode, ''))) = LOWER(TRIM(:code)))")
    List<HrTblDivision> findActiveBySubareaAndNameOrCode(
            @Param("subareaId") Integer subareaId, @Param("name") String name, @Param("code") String code);
}
