package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Ensures Delegation Management submenu exists under User Management for routing and Role Management.
 */
@Component
public class DelegationManagementMenuSeeder {

    private static final Logger log = LoggerFactory.getLogger(DelegationManagementMenuSeeder.class);
    private static final String SUBMENU_NAME = "Delegation Management";
    private static final String SUBMENU_URL = "delegation-management";
    private static final String PARENT_MENU_NAME = "User Management";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    @PostConstruct
    public void seedDelegationManagementSubMenu() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            CfgTblMenu userManagementMenu = findUserManagementMenu(em);
            if (userManagementMenu == null) {
                log.warn("DelegationManagementMenuSeeder: '{}' menu not found; skipping.", PARENT_MENU_NAME);
                em.getTransaction().rollback();
                return;
            }

            CfgTblSubMenu delegationSubMenu = findDelegationSubMenu(em, userManagementMenu.getSerMenuId());
            if (delegationSubMenu == null) {
                delegationSubMenu = createDelegationSubMenu(em, userManagementMenu);
                log.info("DelegationManagementMenuSeeder: created '{}' submenu (id={}).", SUBMENU_NAME,
                        delegationSubMenu.getSerSubMenuId());
            } else {
                normalizeDelegationSubMenu(delegationSubMenu, userManagementMenu);
                em.merge(delegationSubMenu);
                log.info("DelegationManagementMenuSeeder: normalized '{}' submenu (id={}).", SUBMENU_NAME,
                        delegationSubMenu.getSerSubMenuId());
            }

            seedRolePermissions(em, delegationSubMenu);
            normalizeDisabledDelegationPermissions(em, delegationSubMenu);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error("DelegationManagementMenuSeeder failed: {}", e.getMessage(), e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private CfgTblMenu findUserManagementMenu(EntityManager em) {
        try {
            return em.createQuery(
                    "FROM CfgTblMenu m WHERE LOWER(m.txtMenuName) = LOWER(:name) " +
                            "AND (m.blIsDeleted = false OR m.blIsDeleted IS NULL)",
                    CfgTblMenu.class)
                    .setParameter("name", PARENT_MENU_NAME)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private CfgTblSubMenu findDelegationSubMenu(EntityManager em, Integer menuId) {
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

    private CfgTblSubMenu createDelegationSubMenu(EntityManager em, CfgTblMenu parentMenu) {
        Integer nextOrder = em.createQuery(
                "SELECT COALESCE(MAX(sm.intSubMenuOrder), 0) + 1 FROM CfgTblSubMenu sm " +
                        "WHERE sm.cfgTblMenu.serMenuId = :menuId",
                Integer.class)
                .setParameter("menuId", parentMenu.getSerMenuId())
                .getSingleResult();

        CfgTblSubMenu subMenu = new CfgTblSubMenu();
        subMenu.setCfgTblMenu(parentMenu);
        subMenu.setTxtSubMenuName(SUBMENU_NAME);
        subMenu.setTxtSubMenuUrl(SUBMENU_URL);
        subMenu.setIntSubMenuOrder(nextOrder != null ? nextOrder : 1);
        subMenu.setBlIsActive(true);
        subMenu.setBlnStatus(true);
        subMenu.setBlIsDeleted(false);
        subMenu.setBlIsview(true);
        subMenu.setBlIsAdd(true);
        subMenu.setBlIsDelete(true);
        subMenu.setBlIsUpdate(true);
        subMenu.setBlIsApprove(false);
        subMenu.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
        subMenu.setSerCreatedUser(1);
        em.persist(subMenu);
        em.flush();
        return subMenu;
    }

    private void normalizeDelegationSubMenu(CfgTblSubMenu subMenu, CfgTblMenu parentMenu) {
        subMenu.setCfgTblMenu(parentMenu);
        subMenu.setTxtSubMenuName(SUBMENU_NAME);
        subMenu.setTxtSubMenuUrl(SUBMENU_URL);
        subMenu.setBlIsActive(true);
        subMenu.setBlnStatus(true);
        subMenu.setBlIsDeleted(false);
        subMenu.setBlIsview(true);
        subMenu.setBlIsAdd(true);
        subMenu.setBlIsDelete(true);
        subMenu.setBlIsUpdate(true);
        subMenu.setBlIsApprove(false);
        subMenu.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
    }

    @SuppressWarnings("unchecked")
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

            Long existingCount = em.createQuery(
                    "SELECT COUNT(smr) FROM CfgTblSubMenuRole smr " +
                            "WHERE smr.cfgTblSubMenu.serSubMenuId = :subMenuId " +
                            "AND smr.cfgTblRole.serRoleId = :roleId " +
                            "AND smr.cfgTblUser IS NULL",
                    Long.class)
                    .setParameter("subMenuId", subMenu.getSerSubMenuId())
                    .setParameter("roleId", role.getSerRoleId())
                    .getSingleResult();

            if (existingCount != null && existingCount > 0) {
                continue;
            }

            boolean isAdminRole = isAdminRoleName(role.getTxtRoleName());
            CfgTblSubMenuRole permission = new CfgTblSubMenuRole();
            permission.setCfgTblSubMenu(subMenu);
            permission.setCfgTblRole(role);
            permission.setCfgTblUser(null);
            permission.setBlIsActive(true);
            permission.setBlnStatus(true);
            permission.setBlIsDeleted(false);
            permission.setBlIsview(isAdminRole);
            permission.setBlIsAdd(isAdminRole);
            permission.setBlIsDelete(isAdminRole);
            permission.setBlIsUpdate(isAdminRole);
            permission.setBlIsApprove(false);
            permission.setBlIsEnabled(isAdminRole);
            permission.setBlIsAll(false);
            permission.setBlIsNewView(isAdminRole);
            permission.setBlIsNewUpdate(isAdminRole);
            permission.setBlIsNewCreate(isAdminRole);
            permission.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
            permission.setSerCreatedUser(1);
            em.persist(permission);
        }
    }

    private boolean isAdminRoleName(String roleName) {
        if (roleName == null) {
            return false;
        }
        String normalized = roleName.trim().toUpperCase();
        return normalized.contains("ADMIN");
    }

    /**
     * Rows seeded with blIsEnabled=false still had view flags on; sidebar treated blnStatus as access.
     * Clear grant flags when the row is not enabled (non-admin defaults).
     */
    @SuppressWarnings("unchecked")
    private void normalizeDisabledDelegationPermissions(EntityManager em, CfgTblSubMenu subMenu) {
        List<CfgTblSubMenuRole> rows = em.createQuery(
                "FROM CfgTblSubMenuRole smr WHERE smr.cfgTblSubMenu.serSubMenuId = :subMenuId " +
                        "AND smr.cfgTblUser IS NULL",
                CfgTblSubMenuRole.class)
                .setParameter("subMenuId", subMenu.getSerSubMenuId())
                .getResultList();

        for (CfgTblSubMenuRole row : rows) {
            if (row == null || Boolean.TRUE.equals(row.getBlIsDeleted())) {
                continue;
            }
            if (row.getCfgTblRole() != null && isAdminRoleName(row.getCfgTblRole().getTxtRoleName())) {
                continue;
            }
            if (Boolean.TRUE.equals(row.getBlIsEnabled())) {
                continue;
            }
            row.setBlIsview(false);
            row.setBlIsNewView(false);
            row.setBlIsAdd(false);
            row.setBlIsNewCreate(false);
            row.setBlIsDelete(false);
            row.setBlIsUpdate(false);
            row.setBlIsNewUpdate(false);
            em.merge(row);
        }
    }
}
