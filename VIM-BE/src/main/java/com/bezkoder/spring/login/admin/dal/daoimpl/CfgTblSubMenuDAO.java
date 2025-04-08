package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


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

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSubMenu> getAllSubMenu() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<CfgTblSubMenu> SubMenus = entityManager.createQuery("FROM CfgTblSubMenu where blIsDeleted=FALSE or blIsDeleted is null ")
				.getResultList();

	//	entityManager.getTransaction().commit();
	//	entityManager.close();

		return SubMenus;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSubMenu> getActiveSubMenu() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblSubMenu> SubMenus = entityManager
				.createQuery("FROM CfgTblSubMenu where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SubMenus;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSubMenu> getSubMenuByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSubMenu where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serSubMenuId <> " + oldValue;
			}
			List<CfgTblSubMenu> SubMenus = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return SubMenus;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
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
		try {
			entityManager.getTransaction().begin();
			CfgTblSubMenu.setBlnStatus(true);
			CfgTblSubMenu.setBlIsDeleted(false);
			
			entityManager.persist(CfgTblSubMenu);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteSubMenu(List<String> SubMenusId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serSubMenuId : SubMenusId) {
				CfgTblSubMenu SubMenu = entityManager.find(CfgTblSubMenu.class, Integer.parseInt(serSubMenuId));
				if (SubMenu != null) {
					SubMenu.setBlIsDeleted(true);

				}
			}
			entityManager.getTransaction().commit();
			entityManager.close();

		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
			return "Failure";
		}
		return "Success";
	}

	@Override
	public String updateSubMenu(CfgTblSubMenu CfgTblSubMenu) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblSubMenu);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
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
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSubMenu where txtSubMenuCode='" + SubMenuId + "'";

			List<CfgTblSubMenu> SubMenu = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (SubMenu.size() > 0) {
				return String.valueOf(SubMenu.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblSubMenu> searchSubMenu(CfgTblSubMenu SubMenu) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblSubMenu SubMenu where 1=1 ";
	   
	    if(SubMenu.getTxtSubMenuName() !=null){
	    	query+=" and upper(SubMenu.txtSubMenuName) like"+" upper('"+SubMenu.getTxtSubMenuName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(SubMenu.getTxtEmail() !=null){
	    	query+=" and upper(SubMenu.txtEmail) like"+" upper('"+SubMenu.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(SubMenu.getSerSubMenuId() !=null){
	    	query+=" and SubMenu.serSubMenuId ="+" "+SubMenu.getSerSubMenuId()+""+"  ";
	    }
	  
	    query+=" order by SubMenu.serSubMenuId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblSubMenu> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
