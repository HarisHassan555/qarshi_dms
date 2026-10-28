package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SapHcmEmployeeRepository extends JpaRepository<CfgTblSapHcmEmployee, Integer> {

    Optional<CfgTblSapHcmEmployee> findByTxtEmployeeIdAndBlIsDeletedFalse(String txtEmployeeId);

    List<CfgTblSapHcmEmployee> findByBlIsDeletedFalseOrderByTxtEmpNameAsc();
}
