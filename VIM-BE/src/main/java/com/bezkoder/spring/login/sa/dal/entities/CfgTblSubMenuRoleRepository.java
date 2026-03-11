package com.bezkoder.spring.login.sa.dal.entities;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CfgTblSubMenuRoleRepository extends JpaRepository<CfgTblSubMenuRole, Integer> {

    List<CfgTblSubMenuRole> findCfgTblSubMenuRoleByCfgTblRoleAndCfgTblUser(CfgTblRole cfgTblRole, CfgTblUser cfgTblUser);

    /**
     * Fetch role-based permissions, allowing either user-specific rows or role-only rows (ser_user_id IS NULL).
     * Ensures role filter always applies.
     */
    @Query("SELECT r FROM CfgTblSubMenuRole r WHERE r.cfgTblRole = :role AND (r.cfgTblUser = :user OR r.cfgTblUser IS NULL)")
    List<CfgTblSubMenuRole> findByRoleWithUserOrNull(@Param("role") CfgTblRole role, @Param("user") CfgTblUser user);

}
