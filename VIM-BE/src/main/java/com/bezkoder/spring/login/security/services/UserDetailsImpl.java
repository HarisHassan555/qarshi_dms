package com.bezkoder.spring.login.security.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.models.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDetailsImpl implements UserDetails {
  private static final long serialVersionUID = 1L;

  private Long id;

  private String username;

  private String email;

  @JsonIgnore
  private String password;

  private boolean active;

  private Collection<? extends GrantedAuthority> authorities;

  public UserDetailsImpl(Long id, String username, String email, String password,
      boolean active,
      Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.password = password;
    this.active = active;
    this.authorities = authorities;
  }

  public static UserDetailsImpl build(CfgTblUser user) {
    /*List<GrantedAuthority> authorities = user.getCfgTblUserRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getCfgTblRole().getTxtRoleName()))
        .collect(Collectors.toList());*/

    return new UserDetailsImpl(
        Long.valueOf(user.getSerUserId()),
        user.getEffectiveLoginName(),
        user.getTxtAddress(),
        user.getTxtPassword(),
        isUserActive(user),
        new ArrayList<>());
  }

  private static boolean isUserActive(CfgTblUser user) {
    if (Boolean.TRUE.equals(user.getBlIsDeleted())) {
      return false;
    }

    if (user.getBlIsActive() != null) {
      return Boolean.TRUE.equals(user.getBlIsActive());
    }

    if (user.getBlnStatus() != null) {
      return Boolean.TRUE.equals(user.getBlnStatus());
    }

    return true;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    UserDetailsImpl user = (UserDetailsImpl) o;
    return Objects.equals(id, user.id);
  }
}
