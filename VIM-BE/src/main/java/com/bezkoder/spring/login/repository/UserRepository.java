package com.bezkoder.spring.login.repository;

import java.util.Optional;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<CfgTblUser, Long> {
  Optional<CfgTblUser> findByTxtUserName(String username);

  /*Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);*/
}
