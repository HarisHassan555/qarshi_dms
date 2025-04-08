package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.dal.dao.ICfgTblRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.sa.dal.entities.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin("*")
public class PermissionController {

    @Autowired
    private ICfgTblRoleDAO cfgTblRoleDAO;

    @Autowired
    private com.bezkoder.spring.login.sa.dal.entities.PermissionRepository permissionRepository;

    @RequestMapping(value = "/getAllRoles", method = RequestMethod.GET)
    public List<CfgTblRole> getAllRoles() {
        return cfgTblRoleDAO.getAllRole();
    }

    @RequestMapping(value = "/getAllPermission", method = RequestMethod.GET)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

}
