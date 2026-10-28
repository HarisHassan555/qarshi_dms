package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HrTblDivisionRepository extends JpaRepository<HrTblDivision, Integer> {

    List<HrTblDivision> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderByTxtDivisionNameAsc();

    @Query("SELECT d FROM HrTblDivision d WHERE d.serSubareaId = :subareaId "
            + "AND LOWER(TRIM(d.txtDivisionName)) = LOWER(TRIM(:name)) "
            + "AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false)")
    Optional<HrTblDivision> findActiveBySubareaAndNameIgnoreCase(
            @Param("subareaId") Integer subareaId, @Param("name") String name);
}
