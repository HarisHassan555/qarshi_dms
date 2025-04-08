package com.bezkoder.spring.login.sa.dal.entities;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;

import javax.persistence.*;
import java.util.Set;

@Entity
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

   /* @ManyToMany(mappedBy = "permissions")*/
   @ManyToMany
   @JoinTable(
           name = "role_permission",
           joinColumns = @JoinColumn(name = "permission_id"),
           inverseJoinColumns = @JoinColumn(name = "role_id")
   )
    private Set<CfgTblRole> roles;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<CfgTblRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<CfgTblRole> roles) {
        this.roles = roles;
    }
}
