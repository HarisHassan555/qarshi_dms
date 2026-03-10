package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

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
	private static final Map<Character, Character> CYRILLIC_TO_LATIN_LOOKALIKE = new HashMap<>();
	static {
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0410', 'A'); // А
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0412', 'B'); // В
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0415', 'E'); // Е
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u041A', 'K'); // К
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u041C', 'M'); // М
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u041D', 'H'); // Н
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u041E', 'O'); // О
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0420', 'P'); // Р
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0421', 'C'); // С
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0422', 'T'); // Т
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0425', 'X'); // Х
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0423', 'Y'); // У
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0430', 'a'); // а
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0435', 'e'); // е
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u043E', 'o'); // о
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0440', 'p'); // р
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0441', 'c'); // с
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0445', 'x'); // х
		CYRILLIC_TO_LATIN_LOOKALIKE.put('\u0443', 'y'); // у
	}

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
		try {
			entityManager.getTransaction().begin();
			// Use JOIN FETCH to eagerly load users and department head with departments
			List<HrTblDepartment> Departments = entityManager.createQuery(
					"SELECT DISTINCT d FROM HrTblDepartment d " +
					"LEFT JOIN FETCH d.cfgTblUsers " +
					"WHERE d.blIsDeleted = false OR d.blIsDeleted IS NULL")
					.getResultList();

			entityManager.getTransaction().commit();
			return Departments;
		} catch (Exception e) {
			if (entityManager.getTransaction().isActive()) {
				entityManager.getTransaction().rollback();
			}
			log.error("Error getting all departments: " + e.getMessage(), e);
			throw e;
		} finally {
			entityManager.close();
		}
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
			HrTblDepartment.setTxtDepartmentName(normalizeDepartmentText(HrTblDepartment.getTxtDepartmentName()));
			HrTblDepartment.setTxtDepartmentCode(normalizeDepartmentText(HrTblDepartment.getTxtDepartmentCode()));
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
			HrTblDepartment managedDepartment = entityManager.find(HrTblDepartment.class, HrTblDepartment.getSerDepartmentId());
			if (managedDepartment == null) {
				entityManager.getTransaction().rollback();
				return "Failure";
			}

			managedDepartment.setTxtDepartmentName(normalizeDepartmentText(HrTblDepartment.getTxtDepartmentName()));
			managedDepartment.setTxtDepartmentCode(normalizeDepartmentText(HrTblDepartment.getTxtDepartmentCode()));
			managedDepartment.setTxtDescription(HrTblDepartment.getTxtDescription());
			managedDepartment.setSerParentDepartmentId(HrTblDepartment.getSerParentDepartmentId());
			managedDepartment.setSerDepartmentHeadId(HrTblDepartment.getSerDepartmentHeadId());
			managedDepartment.setBlnStatus(HrTblDepartment.getBlnStatus());
			managedDepartment.setBlIsActive(HrTblDepartment.getBlIsActive());
			managedDepartment.setBlIsDeleted(HrTblDepartment.getBlIsDeleted());
			managedDepartment.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
			managedDepartment.setSerModifiedUser(commonService.getCurrentLoggedInUser());
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	private String normalizeDepartmentText(String input) {
		if (input == null) return null;
		String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC).trim();
		StringBuilder sb = new StringBuilder(normalized.length());
		for (char ch : normalized.toCharArray()) {
			sb.append(CYRILLIC_TO_LATIN_LOOKALIKE.getOrDefault(ch, ch));
		}
		return sb.toString().replaceAll("\\s+", " ");
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
