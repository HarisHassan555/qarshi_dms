package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.SimpleDateFormat;

import javax.persistence.*;

import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.dto.NavigationMenuRoles;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblSubMenuRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenu;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblSubMenuRole;


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

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSubMenuRole> getAllSubMenuRole() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<CfgTblSubMenuRole> SubMenuRoles = entityManager.createQuery("FROM CfgTblSubMenuRole where blIsDeleted=FALSE or blIsDeleted is null ")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SubMenuRoles;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSubMenuRole> getActiveSubMenuRole() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblSubMenuRole> SubMenuRoles = entityManager
				.createQuery("FROM CfgTblSubMenuRole where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return SubMenuRoles;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblSubMenuRole> getSubMenuRoleByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSubMenuRole where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serSubMenuRoleId <> " + oldValue;
			}
			List<CfgTblSubMenuRole> SubMenuRoles = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return SubMenuRoles;
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
	public String addNewSubMenuRole(CfgTblSubMenuRole CfgTblSubMenuRole) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblSubMenuRole.setBlnStatus(true);
			CfgTblSubMenuRole.setBlIsDeleted(false);
			
			entityManager.persist(CfgTblSubMenuRole);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteSubMenuRole(List<String> SubMenuRolesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serSubMenuRoleId : SubMenuRolesId) {
				CfgTblSubMenuRole SubMenuRole = entityManager.find(CfgTblSubMenuRole.class, Integer.parseInt(serSubMenuRoleId));
				if (SubMenuRole != null) {
					SubMenuRole.setBlIsDeleted(true);

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
	public String updateSubMenuRole(CfgTblSubMenuRole CfgTblSubMenuRole) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblSubMenuRole);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
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
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblSubMenuRole where txtSubMenuRoleCode='" + SubMenuRoleId + "'";

			List<CfgTblSubMenuRole> SubMenuRole = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (SubMenuRole.size() > 0) {
				return String.valueOf(SubMenuRole.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblSubMenuRole> searchSubMenuRole(CfgTblSubMenuRole SubMenuRole) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblSubMenuRole SubMenuRole where 1=1 ";
	   

	    /*if(SubMenuRole.getTxtEmail() !=null){
	    	query+=" and upper(SubMenuRole.txtEmail) like"+" upper('"+SubMenuRole.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(SubMenuRole.getSerSubMenuRoleId() !=null){
	    	query+=" and SubMenuRole.serSubMenuRoleId ="+" "+SubMenuRole.getSerSubMenuRoleId()+""+"  ";
	    }
	  
	    query+=" order by SubMenuRole.serSubMenuRoleId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblSubMenuRole> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	//@Override
	public String addNewSubMenuRoleinList(List<CfgTblSubMenuRole> lstcfgTblSubMenuRoles) {
		
		CfgTblSubMenuRole cfgTblSubMenuRole_sep;
//		List<CfgTblSubMenuRole> lst_for_updation=new ArrayList();
		List<CfgTblSubMenuRole> lst_for_new=new ArrayList();
		 Iterator<CfgTblSubMenuRole> itr_sep = lstcfgTblSubMenuRoles.iterator();
	      while (itr_sep.hasNext())
	      {
	    	  cfgTblSubMenuRole_sep = (CfgTblSubMenuRole) itr_sep.next();
	    	  if(cfgTblSubMenuRole_sep.getSerSubMenuRoleId()!=null)
	    	  {
				  cfgTblSubMenuRole_sep.setSerCreatedUser(commonService.getCurrentLoggedInUser());
				  //CfgTblRole cfgTblRole = cfgTblSubMenuRole_sep.getCfgTblRole();
//	    		  lst_for_updation.add(cfgTblSubMenuRole_sep);
	    		  updateSubMenuRole(cfgTblSubMenuRole_sep);
	    	  }
	    	  else
	    	  {
				  cfgTblSubMenuRole_sep.setSerCreatedUser(commonService.getCurrentLoggedInUser());
	    		  lst_for_new.add(cfgTblSubMenuRole_sep);
	    	  }
	      }
		
		if(lst_for_new!=null && lst_for_new.size() >0 )
		{
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				CfgTblSubMenuRole cfgTblSubMenuRole;
				 Iterator<CfgTblSubMenuRole> itr = lst_for_new.iterator();
			      while (itr.hasNext())
			      {	
					cfgTblSubMenuRole = (CfgTblSubMenuRole) itr.next();
					cfgTblSubMenuRole.setBlnStatus(true);
					cfgTblSubMenuRole.setBlIsDeleted(false);
					cfgTblSubMenuRole.setSerCreatedUser(commonService.getCurrentLoggedInUser());
					cfgTblSubMenuRole.setDteCreatedDate(commonService.getCurrentTimeStamp_new());
					entityManager.persist(cfgTblSubMenuRole);
					// addNewSubMenuRole(cfgTblSubMenuRole);
			      }
				
				entityManager.getTransaction().commit();
				entityManager.close();
				
				

				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		/*	CfgTblSubMenuRole cfgTblSubMenuRole;
			 Iterator<CfgTblSubMenuRole> itr = lstcfgTblSubMenuRoles.iterator();
		      while (itr.hasNext())
		      {
		    	  cfgTblSubMenuRole = (CfgTblSubMenuRole)itr.next();
		    	  addNewSubMenuRole(cfgTblSubMenuRole);
		      }*/
		      
		      
		
			
		}
		else
			return "Success";
	
	}
}
