package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblSubMenuRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

@Repository
public class CfgTblSubMenuRoleDAO implements ICfgTblSubMenuRoleDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblSubMenuRoleDAO.class);

	public CfgTblSubMenuRoleDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	private void rollbackQuietly(EntityTransaction transaction) {
		if (transaction != null && transaction.isActive()) {
			transaction.rollback();
		}
	}

	private void closeQuietly(EntityManager entityManager) {
		if (entityManager != null && entityManager.isOpen()) {
			entityManager.close();
		}
	}

	@Override
	public List<CfgTblSubMenuRole> getAllSubMenuRole() {
		EntityManager entityManager = getEntityManager();
		try {
			return entityManager.createQuery(
					"FROM CfgTblSubMenuRole where blIsDeleted=FALSE or blIsDeleted is null order by serSubMenuRoleId DESC",
					CfgTblSubMenuRole.class)
				.setHint("org.hibernate.readOnly", true)
				.getResultList();
		} catch (Exception e) {
			log.error("Error fetching all sub menu roles", e);
			throw e;
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public List<CfgTblSubMenuRole> getActiveSubMenuRole() {
		EntityManager entityManager = getEntityManager();
		try {
			return entityManager.createQuery(
					"FROM CfgTblSubMenuRole where blnStatus=TRUE and blIsDeleted=FALSE order by serSubMenuRoleId DESC",
					CfgTblSubMenuRole.class)
				.setHint("org.hibernate.readOnly", true)
				.getResultList();
		} catch (Exception e) {
			log.error("Error fetching active sub menu roles", e);
			throw e;
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public List<CfgTblSubMenuRole> getSubMenuRoleByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			String query = "FROM CfgTblSubMenuRole where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serSubMenuRoleId <> " + oldValue;
			}
			return entityManager.createQuery(query, CfgTblSubMenuRole.class).getResultList();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			closeQuietly(entityManager);
		}
	}

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	/*
	 * String date = simpleDateFormat.format(new Date());
	 * System.out.println(date);
	 */
	@Override
	public String addNewSubMenuRole(CfgTblSubMenuRole CfgTblSubMenuRole) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			Integer roleId = CfgTblSubMenuRole.getCfgTblRole() != null ? CfgTblSubMenuRole.getCfgTblRole().getSerRoleId() : null;
			Integer subMenuId = CfgTblSubMenuRole.getCfgTblSubMenu() != null ? CfgTblSubMenuRole.getCfgTblSubMenu().getSerSubMenuId() : null;
			if (roleId == null || subMenuId == null) {
				return "Failure";
			}

			transaction.begin();
			CfgTblSubMenuRole.setBlnStatus(true);
			CfgTblSubMenuRole.setBlIsDeleted(false);
			CfgTblSubMenuRole.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
			if (CfgTblSubMenuRole.getSerCreatedUser() == null) {
				CfgTblSubMenuRole.setSerCreatedUser(commonService.getCurrentLoggedInUser());
			}

			// Reattach FK references by id to avoid transient/detached association issues.
			CfgTblRole roleRef = entityManager.find(CfgTblRole.class, roleId);
			CfgTblSubMenu subMenuRef = entityManager.find(CfgTblSubMenu.class, subMenuId);
			if (roleRef == null || subMenuRef == null) {
				rollbackQuietly(transaction);
				return "Failure";
			}
			CfgTblSubMenuRole.setCfgTblRole(roleRef);
			CfgTblSubMenuRole.setCfgTblSubMenu(subMenuRef);

			entityManager.persist(CfgTblSubMenuRole);
			transaction.commit();
			return "Success";
		} catch (Exception e) {
			rollbackQuietly(transaction);
			log.error(e.getMessage(), e);
			return "Failure";
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public String deleteSubMenuRole(List<String> SubMenuRolesId) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			for (String serSubMenuRoleId : SubMenuRolesId) {
				CfgTblSubMenuRole SubMenuRole = entityManager.find(CfgTblSubMenuRole.class, Integer.parseInt(serSubMenuRoleId));
				if (SubMenuRole != null) {
					SubMenuRole.setBlIsDeleted(true);
				}
			}
			transaction.commit();

		} catch (Exception ex) {
			rollbackQuietly(transaction);
			log.error(ex.getMessage(), ex);
			return "Failure";
		} finally {
			closeQuietly(entityManager);
		}
		return "Success";
	}

	@Override
	public String deleteSubMenuRoleBySubMenuIds(List<String> subMenuIds) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			List<Integer> parsedIds = new ArrayList<>();
			for (String id : subMenuIds) {
				if (id == null) continue;
				String clean = id.replace("\"", "").trim();
				if (clean.isEmpty()) continue;
				parsedIds.add(Integer.parseInt(clean));
			}

			if (parsedIds.isEmpty()) {
				return "Success";
			}

			transaction.begin();
			entityManager.createQuery(
					"UPDATE CfgTblSubMenuRole smr " +
					"SET smr.blIsDeleted = TRUE, smr.blnStatus = FALSE, smr.blIsActive = FALSE " +
					"WHERE smr.cfgTblSubMenu.serSubMenuId IN :ids")
				.setParameter("ids", parsedIds)
				.executeUpdate();
			transaction.commit();
			return "Success";
		} catch (Exception ex) {
			rollbackQuietly(transaction);
			log.error(ex.getMessage(), ex);
			return "Failure";
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public String updateSubMenuRole(CfgTblSubMenuRole CfgTblSubMenuRole) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			entityManager.merge(CfgTblSubMenuRole);
			transaction.commit();
			return "Success";
		} catch (Exception e) {
			rollbackQuietly(transaction);
			log.error(e.getMessage(), e);
			return "Failure";
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public String generateSubMenuRoleNo(String type) {

		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getSubMenuRoleById(String SubMenuRoleId) {
		EntityManager entityManager = getEntityManager();
		try {
			String query = "FROM CfgTblSubMenuRole where txtSubMenuRoleCode='" + SubMenuRoleId + "'";

			List<CfgTblSubMenuRole> SubMenuRole = entityManager.createQuery(query, CfgTblSubMenuRole.class).getResultList();

			if (SubMenuRole.size() > 0) {
				return String.valueOf(SubMenuRole.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public List<CfgTblSubMenuRole> searchSubMenuRole(CfgTblSubMenuRole SubMenuRole) {
		EntityManager entityManager = getEntityManager();
		try {
			String query = "from CfgTblSubMenuRole SubMenuRole where 1=1 ";

			if (SubMenuRole.getSerSubMenuRoleId() != null) {
				query += " and SubMenuRole.serSubMenuRoleId =" + " " + SubMenuRole.getSerSubMenuRoleId() + "" + "  ";
			}

			query += " order by SubMenuRole.serSubMenuRoleId  DESC";
			log.info("Query is ---" + query.substring(0, query.length()));

			System.out.println("query ----:" + query.substring(0, query.length()));
			String subQuery = query.substring(0, query.length());
			return entityManager.createQuery(subQuery, CfgTblSubMenuRole.class)
					.setHint("org.hibernate.readOnly", true)
					.getResultList();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			closeQuietly(entityManager);
		}
	}

	//@Override
	public String addNewSubMenuRoleinList(List<CfgTblSubMenuRole> lstcfgTblSubMenuRoles) {
		if (lstcfgTblSubMenuRoles == null || lstcfgTblSubMenuRoles.isEmpty()) {
			return "Success";
		}

		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();

			for (CfgTblSubMenuRole incoming : lstcfgTblSubMenuRoles) {
				Integer roleId = incoming.getCfgTblRole() != null ? incoming.getCfgTblRole().getSerRoleId() : null;
				Integer subMenuId = incoming.getCfgTblSubMenu() != null ? incoming.getCfgTblSubMenu().getSerSubMenuId() : null;
				Integer userId = incoming.getCfgTblUser() != null ? incoming.getCfgTblUser().getSerUserId() : null;

				// Skip invalid items instead of failing entire batch.
				if (roleId == null || subMenuId == null) {
					continue;
				}

				CfgTblRole roleRef = entityManager.find(CfgTblRole.class, roleId);
				CfgTblSubMenu subMenuRef = entityManager.find(CfgTblSubMenu.class, subMenuId);
				CfgTblUser userRef = userId != null ? entityManager.find(CfgTblUser.class, userId) : null;
				if (roleRef == null || subMenuRef == null) {
					continue;
				}

				List<CfgTblSubMenuRole> existingRows = entityManager.createQuery(
						"SELECT r FROM CfgTblSubMenuRole r " +
								"WHERE r.cfgTblRole.serRoleId = :roleId " +
								"AND r.cfgTblSubMenu.serSubMenuId = :subMenuId " +
								"AND ((:userId IS NULL AND r.cfgTblUser IS NULL) OR r.cfgTblUser.serUserId = :userId)",
						CfgTblSubMenuRole.class)
					.setParameter("roleId", roleId)
					.setParameter("subMenuId", subMenuId)
					.setParameter("userId", userId)
					.getResultList();

				CfgTblSubMenuRole target;
				if (existingRows != null && !existingRows.isEmpty()) {
					target = existingRows.get(0);
					target.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
					target.setSerModifiedUser(commonService.getCurrentLoggedInUser());
				} else {
					target = new CfgTblSubMenuRole();
					target.setCfgTblRole(roleRef);
					target.setCfgTblSubMenu(subMenuRef);
					target.setCfgTblUser(userRef);
					target.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
					target.setSerCreatedUser(commonService.getCurrentLoggedInUser());
				}

				target.setBlnStatus(true);
				target.setBlIsDeleted(false);
				target.setBlIsActive(incoming.getBlIsActive() != null ? incoming.getBlIsActive() : true);
				target.setBlIsEnabled(incoming.getBlIsEnabled() != null ? incoming.getBlIsEnabled() : true);
				target.setBlIsview(incoming.getBlIsview() != null ? incoming.getBlIsview() : false);
				target.setBlIsAdd(incoming.getBlIsAdd() != null ? incoming.getBlIsAdd() : false);
				target.setBlIsDelete(incoming.getBlIsDelete() != null ? incoming.getBlIsDelete() : false);
				target.setBlIsUpdate(incoming.getBlIsUpdate() != null ? incoming.getBlIsUpdate() : false);
				target.setBlIsApprove(incoming.getBlIsApprove() != null ? incoming.getBlIsApprove() : false);
				target.setBlIsAll(incoming.getBlIsAll() != null ? incoming.getBlIsAll() : false);
				target.setBlIsNewCreate(incoming.getBlIsNewCreate() != null ? incoming.getBlIsNewCreate() : false);
				target.setBlIsNewView(incoming.getBlIsNewView() != null ? incoming.getBlIsNewView() : false);
				target.setBlIsNewUpdate(incoming.getBlIsNewUpdate() != null ? incoming.getBlIsNewUpdate() : false);

				if (target.getSerSubMenuRoleId() == null) {
					entityManager.persist(target);
				} else {
					entityManager.merge(target);
				}
			}

			transaction.commit();
			return "Success";
		} catch (Exception e) {
			rollbackQuietly(transaction);
			log.error(e.getMessage(), e);
			return "Failure";
		} finally {
			closeQuietly(entityManager);
		}
	}
}
