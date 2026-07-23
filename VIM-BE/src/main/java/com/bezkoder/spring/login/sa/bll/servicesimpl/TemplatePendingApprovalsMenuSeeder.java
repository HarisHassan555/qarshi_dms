package com.bezkoder.spring.login.sa.bll.servicesimpl;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import java.util.List;

@Component
@DependsOn("departmentApplicationMenuSeeder")
public class TemplatePendingApprovalsMenuSeeder {

    private static final Logger log = LoggerFactory.getLogger(TemplatePendingApprovalsMenuSeeder.class);
    private static final String SUBMENU_NAME = "Template Pending Approvals";
    private static final String SUBMENU_URL = "template-pending-approvals";

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ICommonService commonService;

    @PostConstruct
    public void seedTemplatePendingApprovalsSubMenu() {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            em.getTransaction().begin();

            CfgTblMenu velocityMenu = findVelocityMenu(em);
            if (velocityMenu == null) {
                log.warn("TemplatePendingApprovalsMenuSeeder: Velocity/VIM menu not found; skipping submenu seed.");
                em.getTransaction().rollback();
                return;
            }

            CfgTblSubMenu subMenu = findTemplatePendingApprovalsSubMenu(em, velocityMenu.getSerMenuId());
            if (subMenu == null) {
                subMenu = createTemplatePendingApprovalsSubMenu(em, velocityMenu);
                log.info("TemplatePendingApprovalsMenuSeeder: created '{}' submenu (id={}).", SUBMENU_NAME,
                        subMenu.getSerSubMenuId());
            } else {
                normalizeTemplatePendingApprovalsSubMenu(subMenu, velocityMenu);
                em.merge(subMenu);
                log.info("TemplatePendingApprovalsMenuSeeder: normalized '{}' submenu (id={}).", SUBMENU_NAME,
                        subMenu.getSerSubMenuId());
            }

            seedRolePermissions(em, subMenu);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            log.error("TemplatePendingApprovalsMenuSeeder failed: {}", e.getMessage(), e);
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

    private CfgTblSubMenu findTemplatePendingApprovalsSubMenu(EntityManager em, Integer menuId) {
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

    private CfgTblSubMenu createTemplatePendingApprovalsSubMenu(EntityManager em, CfgTblMenu parentMenu) {
        Integer nextOrder = em.createQuery(
                "SELECT COALESCE(MAX(sm.intSubMenuOrder), 0) + 1 FROM CfgTblSubMenu sm " +
                        "WHERE sm.cfgTblMenu.serMenuId = :menuId",
                Integer.class)
                .setParameter("menuId", parentMenu.getSerMenuId())
                .getSingleResult();

        CfgTblSubMenu subMenu = new CfgTblSubMenu();
        normalizeTemplatePendingApprovalsSubMenu(subMenu, parentMenu);
        subMenu.setIntSubMenuOrder(nextOrder != null ? nextOrder : 1);
        subMenu.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
        subMenu.setSerCreatedUser(1);
        em.persist(subMenu);
        em.flush();
        return subMenu;
    }

    private void normalizeTemplatePendingApprovalsSubMenu(CfgTblSubMenu subMenu, CfgTblMenu parentMenu) {
        subMenu.setCfgTblMenu(parentMenu);
        subMenu.setTxtSubMenuName(SUBMENU_NAME);
        subMenu.setTxtSubMenuUrl(SUBMENU_URL);
        subMenu.setBlIsActive(true);
        subMenu.setBlnStatus(true);
        subMenu.setBlIsDeleted(false);
        subMenu.setBlIsview(true);
        subMenu.setBlIsAdd(false);
        subMenu.setBlIsDelete(false);
        subMenu.setBlIsUpdate(true);
        subMenu.setBlIsApprove(true);
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

            if (!existingPermissions.isEmpty()) {
                continue;
            }

            boolean isAdminRole = isAdminRoleName(role.getTxtRoleName());
            CfgTblSubMenuRole permission = new CfgTblSubMenuRole();
            permission.setCfgTblSubMenu(subMenu);
            permission.setCfgTblRole(role);
            permission.setCfgTblUser(null);
            permission.setBlIsActive(isAdminRole);
            permission.setBlnStatus(isAdminRole);
            permission.setBlIsDeleted(false);
            permission.setBlIsview(true);
            permission.setBlIsAdd(false);
            permission.setBlIsDelete(false);
            permission.setBlIsUpdate(true);
            permission.setBlIsApprove(true);
            permission.setBlIsEnabled(isAdminRole);
            permission.setBlIsAll(false);
            permission.setBlIsNewView(true);
            permission.setBlIsNewUpdate(true);
            permission.setBlIsNewCreate(false);
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
        return normalized.equals("ADMIN") || normalized.equals("ROLE_ADMIN")
                || normalized.equals("SUPER ADMIN") || normalized.equals("ROLE_SUPER ADMIN")
                || normalized.contains("ADMIN");
    }
}
