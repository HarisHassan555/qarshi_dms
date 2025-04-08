package com.bezkoder.spring.login.sa.dal.entities;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CfgTblSubMenuRoleRepository extends JpaRepository<CfgTblSubMenuRole, Integer> {

    List<CfgTblSubMenuRole> findCfgTblSubMenuRoleByCfgTblRoleAndCfgTblUser(CfgTblRole cfgTblRole, CfgTblUser cfgTblUser);

}