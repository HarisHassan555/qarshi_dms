package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface CfgTblBackgroundJobRepository extends JpaRepository<CfgTblBackgroundJob, Long> {

    @Query("SELECT j FROM CfgTblBackgroundJob j WHERE j.txtStatus = 'PENDING' AND j.dteRunAfter <= :now "
            + "AND j.blIsDeleted = false ORDER BY j.serBackgroundJobId ASC")
    List<CfgTblBackgroundJob> findDuePending(@Param("now") Timestamp now, Pageable pageable);
}
