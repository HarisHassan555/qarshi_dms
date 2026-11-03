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
import java.util.Arrays;
import java.util.List;

@Component
@DependsOn("departmentApplicationMenuSeeder")
public class TemplatePendingApprovalsMenuSeeder {

    private static final Logger log = LoggerFactory.getLogger(TemplatePendingApprovalsMenuSeeder.class);
    private static final List<TemplateSubMenuConfig> TEMPLATE_SUB_MENUS = Arrays.asList(
            new TemplateSubMenuConfig("Digital Document Builder", "template-builder", true, true, true, false, 10),
            new TemplateSubMenuConfig("Digital Document List", "template-list", true, true, true, false, 11),
            new TemplateSubMenuConfig("Template Fill", "template-fill", true, true, true, false, 12),
            new TemplateSubMenuConfig("Digital Pending Approvals", "template-pending-approvals", true, false, true,
                    true, 13),
            new TemplateSubMenuConfig("Template Approval", "template-approval", true, false, true, true, 14),
            new TemplateSubMenuConfig("My Digital Applications", "my-application", true, true, true, false, 15),
            new TemplateSubMenuConfig("Template Approved Applications", "template-approved-applications", true, false,
                    true, false, 16)
    );

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

            for (TemplateSubMenuConfig config : TEMPLATE_SUB_MENUS) {
                CfgTblSubMenu subMenu = findTemplateSubMenu(em, velocityMenu.getSerMenuId(), config);
                if (subMenu == null) {
                    subMenu = createTemplateSubMenu(em, velocityMenu, config);
                    log.info("TemplatePendingApprovalsMenuSeeder: created '{}' submenu (id={}).", config.subMenuName,
                            subMenu.getSerSubMenuId());
                } else {
                    normalizeTemplateSubMenu(subMenu, velocityMenu, config);
                    em.merge(subMenu);
                    log.info("TemplatePendingApprovalsMenuSeeder: normalized '{}' submenu (id={}).",
                            config.subMenuName, subMenu.getSerSubMenuId());
                }

                seedRolePermissions(em, subMenu, config);
            }
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

    private CfgTblSubMenu findTemplateSubMenu(EntityManager em, Integer menuId, TemplateSubMenuConfig config) {
        try {
            return em.createQuery(
                    "FROM CfgTblSubMenu sm WHERE LOWER(sm.txtSubMenuName) = LOWER(:name) " +
                            "AND sm.cfgTblMenu.serMenuId = :menuId",
                    CfgTblSubMenu.class)
                    .setParameter("name", config.subMenuName)
                    .setParameter("menuId", menuId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            try {
                return em.createQuery(
                        "FROM CfgTblSubMenu sm WHERE LOWER(sm.txtSubMenuUrl) = LOWER(:url)",
                        CfgTblSubMenu.class)
                        .setParameter("url", config.subMenuUrl)
                        .setMaxResults(1)
                        .getSingleResult();
            } catch (NoResultException ex) {
                return null;
            }
        }
    }

    private CfgTblSubMenu createTemplateSubMenu(EntityManager em, CfgTblMenu parentMenu, TemplateSubMenuConfig config) {
        CfgTblSubMenu subMenu = new CfgTblSubMenu();
        normalizeTemplateSubMenu(subMenu, parentMenu, config);
        subMenu.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
        subMenu.setSerCreatedUser(1);
        em.persist(subMenu);
        em.flush();
        return subMenu;
    }

    private void normalizeTemplateSubMenu(CfgTblSubMenu subMenu, CfgTblMenu parentMenu, TemplateSubMenuConfig config) {
        subMenu.setCfgTblMenu(parentMenu);
        subMenu.setTxtSubMenuName(config.subMenuName);
        subMenu.setTxtSubMenuUrl(config.subMenuUrl);
        subMenu.setIntSubMenuOrder(config.subMenuOrder);
        subMenu.setBlIsActive(true);
        subMenu.setBlnStatus(true);
        subMenu.setBlIsDeleted(false);
        subMenu.setBlIsview(config.allowView);
        subMenu.setBlIsAdd(config.allowCreate);
        subMenu.setBlIsDelete(false);
        subMenu.setBlIsUpdate(config.allowUpdate);
        subMenu.setBlIsApprove(config.allowApprove);
        subMenu.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
    }

    private void seedRolePermissions(EntityManager em, CfgTblSubMenu subMenu, TemplateSubMenuConfig config) {
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
            permission.setBlIsview(config.allowView);
            permission.setBlIsAdd(config.allowCreate);
            permission.setBlIsDelete(false);
            permission.setBlIsUpdate(config.allowUpdate);
            permission.setBlIsApprove(config.allowApprove);
            permission.setBlIsEnabled(isAdminRole);
            permission.setBlIsAll(false);
            permission.setBlIsNewView(config.allowView);
            permission.setBlIsNewUpdate(config.allowUpdate);
            permission.setBlIsNewCreate(config.allowCreate);
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

    private static class TemplateSubMenuConfig {
        private final String subMenuName;
        private final String subMenuUrl;
        private final boolean allowView;
        private final boolean allowCreate;
        private final boolean allowUpdate;
        private final boolean allowApprove;
        private final int subMenuOrder;

        private TemplateSubMenuConfig(String subMenuName, String subMenuUrl, boolean allowView,
                                      boolean allowCreate, boolean allowUpdate, boolean allowApprove,
                                      int subMenuOrder) {
            this.subMenuName = subMenuName;
            this.subMenuUrl = subMenuUrl;
            this.allowView = allowView;
            this.allowCreate = allowCreate;
            this.allowUpdate = allowUpdate;
            this.allowApprove = allowApprove;
            this.subMenuOrder = subMenuOrder;
        }
    }
}
