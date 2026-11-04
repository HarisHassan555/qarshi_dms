package com.bezkoder.spring.login.sa.dal.entities;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CfgTblApplicationSettingRepository extends JpaRepository<CfgTblApplicationSetting, Integer> {

    List<CfgTblApplicationSetting> findByBlIsDeletedFalseOrBlIsDeletedIsNullOrderBySerApplicationSettingIdDesc();

    Optional<CfgTblApplicationSetting> findFirstByBlIsDeletedFalseOrBlIsDeletedIsNullOrderBySerApplicationSettingIdDesc();
}
