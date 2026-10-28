package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailLayoutRepository extends JpaRepository<CfgTblEmailLayout, Integer> {

    List<CfgTblEmailLayout> findByBlIsDeletedFalseOrderByTxtLayoutNameAsc();

    Optional<CfgTblEmailLayout> findBySerEmailLayoutIdAndBlIsDeletedFalse(Integer id);

    Optional<CfgTblEmailLayout> findByTxtLayoutCodeIgnoreCaseAndBlIsDeletedFalse(String code);

    boolean existsByTxtLayoutCodeIgnoreCaseAndBlIsDeletedFalseAndSerEmailLayoutIdNot(String code, Integer id);
}
