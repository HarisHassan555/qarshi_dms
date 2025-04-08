package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblDepartmentDAO;
import com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment;

@Repository
public class HrTblDepartmentDAO implements IHrTblDepartmentDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(HrTblDepartmentDAO.class);

	public HrTblDepartmentDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblDepartment> getAllDepartments() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblDepartment> Departments = entityManager.createQuery("FROM HrTblDepartment where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Departments;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblDepartment> getActiveDepartments() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblDepartment> Departments = entityManager
				.createQuery("FROM HrTblDepartment where blStatus=true  and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Departments;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblDepartment> getDepartmentByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblDepartment where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<HrTblDepartment> Departments = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Departments;
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
	public String addNewDepartment(HrTblDepartment HrTblDepartment) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			HrTblDepartment.setBlnStatus(true);
			HrTblDepartment.setBlIsDeleted(false);
			entityManager.persist(HrTblDepartment);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteDepartments(List<String> DepartmentsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serDepartmentId : DepartmentsId) {
				HrTblDepartment Department = entityManager.find(HrTblDepartment.class, Integer.parseInt(serDepartmentId));
				if (Department != null) {
					Department.setBlIsDeleted(true);
					Department.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
					Department.setSerModifiedUser(commonService.getCurrentLoggedInUser());
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
	public String updateDepartment(HrTblDepartment HrTblDepartment) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			HrTblDepartment.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
			HrTblDepartment.setSerModifiedUser(commonService.getCurrentLoggedInUser());
			entityManager.merge(HrTblDepartment);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateDepartmentNo(String type) {
		// int DepartmentNo;
		String DepartmentType = type;
		// String DepartmentCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (DepartmentType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDepartmentCode) from HrTblDepartment ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "BK-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "BK-1";
				if (ord_no < 10)
					code = "BK-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "BK-0" + ord_no;
				else
					code = "BK-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtDepartmentCode) from HrTblDepartment ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "DPT-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "DPT-1";
				if (ord_no1 < 10)
					code = "DPT-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "DPT-0" + ord_no1;
				else
					code = "DPT-" + ord_no1;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
	}

	public static boolean isNullOrEmpty(String myString) {
		return myString == null || "".equals(myString);
	}

	public String getDepartmentById(String DepartmentId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblDepartment where txtDepartmentCode='" + DepartmentId + "'";

			List<HrTblDepartment> Department = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Department.size() > 0) {
				return String.valueOf(Department.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<HrTblDepartment> searchDepartment(HrTblDepartment Department) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from HrTblDepartment Department where 1=1 ";
	    if(Department.getTxtDepartmentCode() != null)
	    {
	    	query+=" and upper(Department.txtDepartmentCode) like"+" upper('"+Department.getTxtDepartmentCode()+"%')"+" ";
	    }
	    if(Department.getTxtDepartmentName() !=null){
	    	query+=" and upper(Department.txtDepartmentName) like"+" upper('"+Department.getTxtDepartmentName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Department.getTxtEmail() !=null){
	    	query+=" and upper(Department.txtEmail) like"+" upper('"+Department.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Department.getSerDepartmentId() !=null){
	    	query+=" and Department.serDepartmentId ="+" "+Department.getSerDepartmentId()+""+"  ";
	    }
	  
	    query+=" order by Department.serDepartmentId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<HrTblDepartment> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
