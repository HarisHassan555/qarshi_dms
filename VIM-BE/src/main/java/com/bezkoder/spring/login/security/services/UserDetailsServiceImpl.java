package com.bezkoder.spring.login.security.services;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bezkoder.spring.login.repository.UserRepository;

import java.util.ArrayList;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
  @Autowired
  UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    CfgTblUser user = userRepository.findByLoginNameForAuthentication(username)
        .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));
     return UserDetailsImpl.build(user);
             //new org.springframework.security.core.userdetails.User(user.getTxtUserName(), user.getTxtPassword(), new ArrayList<>());
            //
  }

}
