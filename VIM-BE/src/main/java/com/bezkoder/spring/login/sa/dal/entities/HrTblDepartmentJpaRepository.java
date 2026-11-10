package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HrTblDepartmentJpaRepository extends JpaRepository<HrTblDepartment, Integer> {

    @Query("SELECT d FROM HrTblDepartment d WHERE d.serDivisionId = :divisionId "
            + "AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false) AND ("
            + "LOWER(TRIM(d.txtDepartmentName)) = LOWER(TRIM(:name)) "
            + "OR LOWER(TRIM(COALESCE(d.txtDepartmentCode, ''))) = LOWER(TRIM(:code)))")
    List<HrTblDepartment> findActiveByDivisionAndNameOrCode(
            @Param("divisionId") Integer divisionId, @Param("name") String name, @Param("code") String code);
}
