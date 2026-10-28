package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HrTblDepartmentJpaRepository extends JpaRepository<HrTblDepartment, Integer> {

    @Query("SELECT d FROM HrTblDepartment d WHERE d.serDivisionId = :divisionId "
            + "AND LOWER(TRIM(d.txtDepartmentName)) = LOWER(TRIM(:name)) "
            + "AND (d.blIsDeleted IS NULL OR d.blIsDeleted = false)")
    Optional<HrTblDepartment> findActiveByDivisionAndNameIgnoreCase(
            @Param("divisionId") Integer divisionId, @Param("name") String name);
}
