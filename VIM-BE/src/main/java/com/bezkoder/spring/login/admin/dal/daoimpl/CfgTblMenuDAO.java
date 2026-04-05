package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblMenuDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblMenu;

@Repository
public class CfgTblMenuDAO implements ICfgTblMenuDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblMenuDAO.class);

	public CfgTblMenuDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblMenu> getAllMenu() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		// Use 'blIsDeleted is false or blIsDeleted is null' so that rows seeded
		// directly via SQL with a NULL value are not silently excluded.
		List<CfgTblMenu> Menus = entityManager
				.createQuery("FROM CfgTblMenu where blIsDeleted = false or blIsDeleted is null")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Menus;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblMenu> getActiveMenu() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblMenu> Menus = entityManager
				.createQuery("FROM CfgTblMenu where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Menus;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblMenu> getMenuByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblMenu where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serMenuId <> " + oldValue;
			}
			List<CfgTblMenu> Menus = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Menus;
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
	public String addNewMenu(CfgTblMenu CfgTblMenu) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblMenu.setBlnStatus(true);
			CfgTblMenu.setBlIsDeleted(false);
			CfgTblMenu.setBlIsActive(true);
			entityManager.persist(CfgTblMenu);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteMenu(List<String> MenusId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serMenuId : MenusId) {
				CfgTblMenu Menu = entityManager.find(CfgTblMenu.class, Integer.parseInt(serMenuId));
				if (Menu != null) {
					Menu.setBlIsDeleted(true);

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
	public String updateMenu(CfgTblMenu CfgTblMenu) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblMenu);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateMenuNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getMenuById(String MenuId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblMenu where txtMenuCode='" + MenuId + "'";

			List<CfgTblMenu> Menu = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Menu.size() > 0) {
				return String.valueOf(Menu.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblMenu> searchMenu(CfgTblMenu Menu) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblMenu Menu where 1=1 ";
	    /*if(Menu.getTxtMenuCode() != null)
	    {
	    	query+=" and upper(Menu.txtMenuCode) like"+" upper('"+Menu.getTxtMenuCode()+"%')"+" ";
	    }*/
	   



	    if(Menu.getTxtMenuName() !=null){
	    	query+=" and upper(Menu.txtMenuName) like"+" upper('"+Menu.getTxtMenuName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Menu.getTxtEmail() !=null){
	    	query+=" and upper(Menu.txtEmail) like"+" upper('"+Menu.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Menu.getSerMenuId() !=null){
	    	query+=" and Menu.serMenuId ="+" "+Menu.getSerMenuId()+""+"  ";
	    }
	  
	    query+=" order by Menu.serMenuId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblMenu> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	  //  entityManager.getTransaction().commit();
	  //  entityManager.close();
	 
	    return cust;
	}
}
