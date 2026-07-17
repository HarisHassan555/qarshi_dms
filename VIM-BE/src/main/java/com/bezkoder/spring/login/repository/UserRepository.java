package com.bezkoder.spring.login.repository;

import java.util.Optional;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<CfgTblUser, Long> {
  @Query("SELECT u FROM CfgTblUser u " +
      "WHERE LOWER(u.txtLoginName) = LOWER(:loginName) " +
      "OR ((u.txtLoginName IS NULL OR TRIM(u.txtLoginName) = '') AND LOWER(u.txtUserName) = LOWER(:loginName))")
  Optional<CfgTblUser> findByLoginNameForAuthentication(@Param("loginName") String loginName);

  /*Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);*/
}
