package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;

@Component
public class DepartmentAdminRoleSeeder {

    private static final Logger log = LoggerFactory.getLogger(DepartmentAdminRoleSeeder.class);
    private static final String ROLE_NAME = "department_admin";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    @PostConstruct
    public void ensureDepartmentAdminRoleExists() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            CfgTblRole role = findDepartmentAdminRole(em);
            if (role == null) {
                role = new CfgTblRole();
                role.setTxtRoleName(ROLE_NAME);
                role.setTxtRoleCode(ROLE_NAME.toUpperCase());
                role.setBlIsActive(true);
                role.setBlnStatus(true);
                role.setBlIsDeleted(false);
                role.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
                role.setSerCreatedUser(1);
                em.persist(role);
                log.info("DepartmentAdminRoleSeeder: created '{}' role.", ROLE_NAME);
            } else {
                role.setTxtRoleName(ROLE_NAME);
                role.setTxtRoleCode(ROLE_NAME.toUpperCase());
                role.setBlIsActive(true);
                role.setBlnStatus(true);
                role.setBlIsDeleted(false);
                role.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                role.setSerModifiedUser(1);
                em.merge(role);
                log.info("DepartmentAdminRoleSeeder: normalized '{}' role (id={}).", ROLE_NAME,
                        role.getSerRoleId());
            }

            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error("DepartmentAdminRoleSeeder failed: {}", e.getMessage(), e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private CfgTblRole findDepartmentAdminRole(EntityManager em) {
        try {
            return em.createQuery(
                    "FROM CfgTblRole r WHERE LOWER(TRIM(r.txtRoleName)) = LOWER(:roleName)",
                    CfgTblRole.class)
                    .setParameter("roleName", ROLE_NAME)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
