package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Ensures Department Application submenu exists under Velocity/VIM so it appears in /roles.
 */
@Component
@DependsOn("departmentAdminRoleSeeder")
public class DepartmentApplicationMenuSeeder {

    private static final Logger log = LoggerFactory.getLogger(DepartmentApplicationMenuSeeder.class);
    private static final String SUBMENU_NAME = "Department Application";
    private static final String SUBMENU_URL = "department-application";
    private static final String DEPARTMENT_ADMIN_ROLE = "DEPARTMENT_ADMIN";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    @PostConstruct
    public void seedDepartmentApplicationSubMenu() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            CfgTblMenu velocityMenu = findVelocityMenu(em);
            if (velocityMenu == null) {
                log.warn("DepartmentApplicationMenuSeeder: Velocity/VIM menu not found; skipping submenu seed.");
                em.getTransaction().rollback();
                return;
            }

            CfgTblSubMenu departmentApplicationSubMenu = findDepartmentApplicationSubMenu(em,
                    velocityMenu.getSerMenuId());
            if (departmentApplicationSubMenu == null) {
                departmentApplicationSubMenu = createDepartmentApplicationSubMenu(em, velocityMenu);
                log.info("DepartmentApplicationMenuSeeder: created '{}' submenu (id={}).", SUBMENU_NAME,
                        departmentApplicationSubMenu.getSerSubMenuId());
            } else {
                normalizeDepartmentApplicationSubMenu(departmentApplicationSubMenu, velocityMenu);
                em.merge(departmentApplicationSubMenu);
                log.info("DepartmentApplicationMenuSeeder: normalized '{}' submenu (id={}).", SUBMENU_NAME,
                        departmentApplicationSubMenu.getSerSubMenuId());
            }

            seedRolePermissions(em, departmentApplicationSubMenu);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error("DepartmentApplicationMenuSeeder failed: {}", e.getMessage(), e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private CfgTblMenu findVelocityMenu(EntityManager em) {
        try {
            return em.createQuery(
                    "FROM CfgTblMenu m WHERE (LOWER(m.txtMenuName) = LOWER(:velocity) " +
                            "OR LOWER(m.txtMenuName) LIKE LOWER(:velocityLike) " +
                            "OR LOWER(m.txtMenuName) LIKE LOWER(:vimLike)) " +
                            "AND (m.blIsDeleted = false OR m.blIsDeleted IS NULL)",
                    CfgTblMenu.class)
                    .setParameter("velocity", "Velocity")
                    .setParameter("velocityLike", "%velocity%")
                    .setParameter("vimLike", "%vim%")
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private CfgTblSubMenu findDepartmentApplicationSubMenu(EntityManager em, Integer menuId) {
        try {
            return em.createQuery(
                    "FROM CfgTblSubMenu sm WHERE LOWER(sm.txtSubMenuName) = LOWER(:name) " +
                            "AND sm.cfgTblMenu.serMenuId = :menuId",
                    CfgTblSubMenu.class)
                    .setParameter("name", SUBMENU_NAME)
                    .setParameter("menuId", menuId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            try {
                return em.createQuery(
                        "FROM CfgTblSubMenu sm WHERE LOWER(sm.txtSubMenuUrl) = LOWER(:url)",
                        CfgTblSubMenu.class)
                        .setParameter("url", SUBMENU_URL)
                        .setMaxResults(1)
                        .getSingleResult();
            } catch (NoResultException ex) {
                return null;
            }
        }
    }

    private CfgTblSubMenu createDepartmentApplicationSubMenu(EntityManager em, CfgTblMenu parentMenu) {
        Integer nextOrder = em.createQuery(
                "SELECT COALESCE(MAX(sm.intSubMenuOrder), 0) + 1 FROM CfgTblSubMenu sm " +
                        "WHERE sm.cfgTblMenu.serMenuId = :menuId",
                Integer.class)
                .setParameter("menuId", parentMenu.getSerMenuId())
                .getSingleResult();

        CfgTblSubMenu subMenu = new CfgTblSubMenu();
        normalizeDepartmentApplicationSubMenu(subMenu, parentMenu);
        subMenu.setIntSubMenuOrder(nextOrder != null ? nextOrder : 1);
        subMenu.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
        subMenu.setSerCreatedUser(1);
        em.persist(subMenu);
        em.flush();
        return subMenu;
    }

    private void normalizeDepartmentApplicationSubMenu(CfgTblSubMenu subMenu, CfgTblMenu parentMenu) {
        subMenu.setCfgTblMenu(parentMenu);
        subMenu.setTxtSubMenuName(SUBMENU_NAME);
        subMenu.setTxtSubMenuUrl(SUBMENU_URL);
        subMenu.setBlIsActive(true);
        subMenu.setBlnStatus(true);
        subMenu.setBlIsDeleted(false);
        subMenu.setBlIsview(true);
        subMenu.setBlIsAdd(false);
        subMenu.setBlIsDelete(false);
        subMenu.setBlIsUpdate(false);
        subMenu.setBlIsApprove(false);
        subMenu.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
    }

    private void seedRolePermissions(EntityManager em, CfgTblSubMenu subMenu) {
        List<CfgTblRole> roles = em.createQuery(
                "FROM CfgTblRole r WHERE (r.blIsDeleted = false OR r.blIsDeleted IS NULL) " +
                        "AND (r.blnStatus = true OR r.blnStatus IS NULL)",
                CfgTblRole.class)
                .getResultList();

        for (CfgTblRole role : roles) {
            if (role == null || role.getSerRoleId() == null) {
                continue;
            }

            boolean isAdminRole = isAdminRoleName(role.getTxtRoleName());
            boolean isDepartmentAdminRole = isDepartmentAdminRoleName(role.getTxtRoleName());
            List<CfgTblSubMenuRole> existingPermissions = em.createQuery(
                    "FROM CfgTblSubMenuRole smr " +
                            "WHERE smr.cfgTblSubMenu.serSubMenuId = :subMenuId " +
                            "AND smr.cfgTblRole.serRoleId = :roleId " +
                            "AND smr.cfgTblUser IS NULL",
                    CfgTblSubMenuRole.class)
                    .setParameter("subMenuId", subMenu.getSerSubMenuId())
                    .setParameter("roleId", role.getSerRoleId())
                    .setMaxResults(1)
                    .getResultList();

            if (!existingPermissions.isEmpty() && !isDepartmentAdminRole) {
                continue;
            }

            CfgTblSubMenuRole permission = existingPermissions.isEmpty() ? new CfgTblSubMenuRole()
                    : existingPermissions.get(0);
            permission.setCfgTblSubMenu(subMenu);
            permission.setCfgTblRole(role);
            permission.setCfgTblUser(null);
            permission.setBlIsActive(true);
            permission.setBlnStatus(true);
            permission.setBlIsDeleted(false);
            permission.setBlIsview(true);
            permission.setBlIsAdd(false);
            permission.setBlIsDelete(false);
            permission.setBlIsUpdate(false);
            permission.setBlIsApprove(false);
            permission.setBlIsEnabled(isAdminRole);
            permission.setBlIsAll(false);
            permission.setBlIsNewView(true);
            permission.setBlIsNewUpdate(false);
            permission.setBlIsNewCreate(false);

            if (permission.getSerSubMenuRoleId() == null) {
                permission.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
                permission.setSerCreatedUser(1);
                em.persist(permission);
            } else {
                permission.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
                permission.setSerModifiedUser(1);
                em.merge(permission);
            }
        }
    }

    private boolean isAdminRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase();
        return normalized.contains("ADMIN") || DEPARTMENT_ADMIN_ROLE.equals(normalized);
    }

    private boolean isDepartmentAdminRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        return DEPARTMENT_ADMIN_ROLE.equals(roleName.trim().toUpperCase());
    }
}
