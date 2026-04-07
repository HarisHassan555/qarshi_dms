package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblSubMenuDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;

@Repository
public class CfgTblSubMenuDAO implements ICfgTblSubMenuDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblSubMenuDAO.class);

	public CfgTblSubMenuDAO() {
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
	public List<CfgTblSubMenu> getAllSubMenu() {
		EntityManager entityManager = getEntityManager();
		try {
			return entityManager.createQuery(
					"FROM CfgTblSubMenu where blIsDeleted=FALSE or blIsDeleted is null order by serSubMenuId DESC",
					CfgTblSubMenu.class)
				.setHint("org.hibernate.readOnly", true)
				.getResultList();
		} catch (Exception e) {
			log.error("Error fetching all sub menus", e);
			throw e;
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public List<CfgTblSubMenu> getActiveSubMenu() {
		EntityManager entityManager = getEntityManager();
		try {
			return entityManager.createQuery(
					"FROM CfgTblSubMenu where blnStatus=TRUE and blIsDeleted=FALSE order by serSubMenuId DESC",
					CfgTblSubMenu.class)
				.setHint("org.hibernate.readOnly", true)
				.getResultList();
		} catch (Exception e) {
			log.error("Error fetching active sub menus", e);
			throw e;
		} finally {
			closeQuietly(entityManager);
		}
	}

	@Override
	public List<CfgTblSubMenu> getSubMenuByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			String query = "FROM CfgTblSubMenu where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serSubMenuId <> " + oldValue;
			}
			return entityManager.createQuery(query, CfgTblSubMenu.class).getResultList();
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
	public String addNewSubMenu(CfgTblSubMenu CfgTblSubMenu) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			CfgTblSubMenu.setBlnStatus(true);
			CfgTblSubMenu.setBlIsDeleted(false);

			entityManager.persist(CfgTblSubMenu);
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
	public String deleteSubMenu(List<String> SubMenusId) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			for (String serSubMenuId : SubMenusId) {
				CfgTblSubMenu SubMenu = entityManager.find(CfgTblSubMenu.class, Integer.parseInt(serSubMenuId));
				if (SubMenu != null) {
					SubMenu.setBlIsDeleted(true);
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
	public String updateSubMenu(CfgTblSubMenu CfgTblSubMenu) {
		EntityManager entityManager = getEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		try {
			transaction.begin();
			entityManager.merge(CfgTblSubMenu);
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
	public String generateSubMenuNo(String type) {

		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getSubMenuById(String SubMenuId) {
		EntityManager entityManager = getEntityManager();
		try {
			String query = "FROM CfgTblSubMenu where txtSubMenuCode='" + SubMenuId + "'";

			List<CfgTblSubMenu> SubMenu = entityManager.createQuery(query, CfgTblSubMenu.class).getResultList();

			if (SubMenu.size() > 0) {
				return String.valueOf(SubMenu.size());
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
	public List<CfgTblSubMenu> searchSubMenu(CfgTblSubMenu SubMenu) {
		EntityManager entityManager = getEntityManager();
		try {
			String query = "from CfgTblSubMenu SubMenu where 1=1 ";

			if (SubMenu.getTxtSubMenuName() != null) {
				query += " and upper(SubMenu.txtSubMenuName) like" + " upper('" + SubMenu.getTxtSubMenuName() + "%')" + "  ";
			}

			if (SubMenu.getSerSubMenuId() != null) {
				query += " and SubMenu.serSubMenuId =" + " " + SubMenu.getSerSubMenuId() + "" + "  ";
			}

			query += " order by SubMenu.serSubMenuId  DESC";
			log.info("Query is ---" + query.substring(0, query.length()));

			System.out.println("query ----:" + query.substring(0, query.length()));
			String subQuery = query.substring(0, query.length());
			return entityManager.createQuery(subQuery, CfgTblSubMenu.class)
					.setHint("org.hibernate.readOnly", true)
					.getResultList();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw e;
		} finally {
			closeQuietly(entityManager);
		}
	}
}
