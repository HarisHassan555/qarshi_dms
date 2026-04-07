package com.bezkoder.spring.login.admin.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblRoleDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblRole;

@Repository
public class CfgTblRoleDAO implements ICfgTblRoleDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblRoleDAO.class);

	public CfgTblRoleDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblRole> getAllRole() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin(); 
		List<CfgTblRole> Roles = entityManager.createQuery("FROM CfgTblRole where blIsDeleted=false")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Roles;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblRole> getActiveRole() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		///*and role.serRoleId in  ( "
		//						+ " select subrole.cfgTblRole.serRoleId FROM CfgTblSubMenuRole subrole where subrole.blIsDeleted=FALSE or subrole.blIsDeleted is null)"*/
		/*List<CfgTblRole> Roles = entityManager
				.createQuery("FROM CfgTblRole role where role.blnStatus=TRUE and role.blIsDeleted=FALSE").getResultList();*/

		List<CfgTblRole> Roles = entityManager
				.createQuery("SELECT DISTINCT role FROM CfgTblRole role " +
						"WHERE role.blnStatus=TRUE AND role.blIsDeleted=FALSE", CfgTblRole.class)
				.getResultList();
		entityManager.getTransaction().commit();
		entityManager.close();

		return Roles;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblRole> getRoleByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblRole where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serRoleId <> " + oldValue;
			}
			List<CfgTblRole> Roles = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Roles;
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
	public String addNewRole(CfgTblRole CfgTblRole) {
		EntityManager entityManager = getEntityManager();
		try {
			String roleName = CfgTblRole.getTxtRoleName() != null ? CfgTblRole.getTxtRoleName().trim() : null;
			String roleCode = CfgTblRole.getTxtRoleCode() != null ? CfgTblRole.getTxtRoleCode().trim() : null;
			if (roleName == null || roleName.isEmpty()) {
				return "Failure";
			}
			if (roleCode == null || roleCode.isEmpty()) {
				roleCode = roleName.toUpperCase().replaceAll("\\s+", "_");
			}

			entityManager.getTransaction().begin();

			// Exact duplicate check by role name/code among non-deleted roles.
			Long duplicateCount = ((Number) entityManager.createNativeQuery(
					"SELECT COUNT(*) FROM cfg_tbl_role r " +
							"WHERE (UPPER(TRIM(r.txt_role_name)) = UPPER(TRIM(:roleName)) " +
							"OR UPPER(TRIM(r.txt_role_code)) = UPPER(TRIM(:roleCode))) " +
							"AND (r.bl_is_deleted = 0 OR r.bl_is_deleted IS NULL)")
				.setParameter("roleName", roleName)
				.setParameter("roleCode", roleCode)
				.getSingleResult()).longValue();
			if (duplicateCount != null && duplicateCount > 0) {
				entityManager.getTransaction().rollback();
				return "EXIST";
			}

			Integer nextId = ((Number) entityManager.createNativeQuery(
					"SELECT COALESCE(MAX(ser_role_id), 0) + 1 FROM cfg_tbl_role")
				.getSingleResult()).intValue();

			Integer createdUser = CfgTblRole.getSerCreatedUser();
			if (createdUser == null || createdUser <= 0) {
				createdUser = commonService.getCurrentLoggedInUser();
			}
			if (createdUser == null || createdUser <= 0) {
				createdUser = 1;
			}

			entityManager.createNativeQuery(
					"INSERT INTO cfg_tbl_role (" +
							"ser_role_id, txt_role_name, txt_role_code, " +
							"bl_is_active, bln_status, bl_is_deleted, dte_created_date, ser_created_user" +
							") VALUES (" +
							":id, :name, :code, :active, :status, :deleted, NOW(), :createdUser" +
							")")
				.setParameter("id", nextId)
				.setParameter("name", roleName)
				.setParameter("code", roleCode)
				.setParameter("active", true)
				.setParameter("status", true)
				.setParameter("deleted", false)
				.setParameter("createdUser", createdUser)
				.executeUpdate();

			entityManager.getTransaction().commit();
			return "Success";
		} catch (Exception e) {
			try {
				if (entityManager.getTransaction().isActive()) {
					entityManager.getTransaction().rollback();
				}
			} catch (Exception ignored) {
			}
			log.error(e.getMessage(), e);
			return "Failure";
		} finally {
			if (entityManager != null && entityManager.isOpen()) {
				entityManager.close();
			}
		}
	}

	@Override
	public String deleteRole(List<String> RolesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serRoleId : RolesId) {
				CfgTblRole Role = entityManager.find(CfgTblRole.class, Integer.parseInt(serRoleId));
				if (Role != null) {
					Role.setBlIsDeleted(true);

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
	public String updateRole(CfgTblRole CfgTblRole) {
		EntityManager entityManager = getEntityManager();
		try {
			CfgTblRole role=new CfgTblRole();
			role.setTxtRoleName(CfgTblRole.getTxtRoleName());
			role.setSerRoleId(CfgTblRole.getSerRoleId());
			List lstRole=searchRoleDuplicate(CfgTblRole);
			if(lstRole!=null && lstRole.size() >0)
			{
				return "EXIST";
			}
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblRole);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateRoleNo(String type) {
		
		return "";
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getRoleById(String RoleId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblRole where txtRoleCode='" + RoleId + "'";

			List<CfgTblRole> Role = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Role.size() > 0) {
				return String.valueOf(Role.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblRole> searchRole(CfgTblRole Role) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblRole Role where 1=1 ";
	    if(Role.getTxtRoleCode() != null)
	    {
	    	query+=" and upper(Role.txtRoleCode) like"+" upper('"+Role.getTxtRoleCode()+"%')"+" ";
	    }
	    if(Role.getTxtRoleName() !=null){
	    	query+=" and upper(Role.txtRoleName) like"+" upper('"+Role.getTxtRoleName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Role.getTxtEmail() !=null){
	    	query+=" and upper(Role.txtEmail) like"+" upper('"+Role.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Role.getSerRoleId() !=null){
	    	query+=" and Role.serRoleId ="+" "+Role.getSerRoleId()+""+"  ";
	    }
	    
	    query+= "and (Role.blIsDeleted=false or Role.blIsDeleted is null) ";
	  
	    query+=" order by Role.serRoleId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblRole> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	
	public List<CfgTblRole> searchRoleDuplicate(CfgTblRole Role) {
		EntityManager entityManager = getEntityManager();
    entityManager.getTransaction().begin();
    String query = "from CfgTblRole Role where 1=1 ";
    if(Role.getTxtRoleCode() != null)
    {
    	query+=" and upper(Role.txtRoleCode) like"+" upper('"+Role.getTxtRoleCode()+"%')"+" ";
    }
    if(Role.getTxtRoleName() !=null){
    	query+=" and upper(Role.txtRoleName) like"+" upper('"+Role.getTxtRoleName()+"%')"+"  ";
    }
    
  
    
    /*if(Role.getTxtEmail() !=null){
    	query+=" and upper(Role.txtEmail) like"+" upper('"+Role.getTxtEmail()+"')"+"  ";
    }*/
    
    if(Role.getSerRoleId() !=null){
    	query+=" and Role.serRoleId !="+" "+Role.getSerRoleId()+""+"  ";
    }
    
    query+= "and (Role.blIsDeleted=false or Role.blIsDeleted is null) ";
  
    query+=" order by Role.serRoleId  DESC";
    log.info("Query is ---"+query.substring(0, query.length()));
    
    System.out.println("query ----:"+query.substring(0, query.length()));
    String subQuery = query.substring(0, query.length());
    List<CfgTblRole> cust = entityManager.createQuery(
    		subQuery).getResultList();
    
    entityManager.getTransaction().commit();
    entityManager.close();
 
    return cust;
}
}
