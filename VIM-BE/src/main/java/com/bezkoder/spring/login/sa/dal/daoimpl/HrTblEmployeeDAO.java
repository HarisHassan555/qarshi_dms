package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.IHrTblEmployeeDAO;

import com.bezkoder.spring.login.sa.dal.entities.HrTblEmployee;

@Repository
public class HrTblEmployeeDAO implements IHrTblEmployeeDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(HrTblEmployeeDAO.class);

	public HrTblEmployeeDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblEmployee> getAllEmployees() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblEmployee> Employees = entityManager.createQuery("FROM HrTblEmployee where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Employees;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblEmployee> getActiveEmployees() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<HrTblEmployee> Employees = entityManager
				.createQuery("FROM HrTblEmployee where blStatus=true  and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Employees;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<HrTblEmployee> getEmployeeByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblEmployee where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<HrTblEmployee> Employees = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Employees;
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
	public String addNewEmployee(HrTblEmployee HrTblEmployee) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			HrTblEmployee.setBlnStatus(true);
			HrTblEmployee.setBlIsDeleted(false);
			if(HrTblEmployee.getHrTblDepartment()!=null)
			{
				if(!(HrTblEmployee.getHrTblDepartment().getSerDepartmentId()!=null))
				{
					HrTblEmployee.setHrTblDepartment(null);
				}
			}
			
			if(HrTblEmployee.getHrTblDesignation()!=null)
			{
				if(!(HrTblEmployee.getHrTblDesignation().getSerDesignationId()!=null))
				{
					HrTblEmployee.setHrTblDesignation(null);
				}
			}
			
			if(HrTblEmployee.getCfgTblSupplier()!=null)
			{
				if(!(HrTblEmployee.getCfgTblSupplier().getSerSupplierId()!=null))
				{
					HrTblEmployee.setCfgTblSupplier(null);
				}
			}
			if(HrTblEmployee.getSerEmployeeId()!=null && HrTblEmployee.getSerEmployeeId()>0)
			{
				try {
					HrTblEmployee.setSerEmployeeId(Integer.parseInt(HrTblEmployee.getTxtEmployeeCode()));
				} 
				catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "format";
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "format";
				}
			entityManager.merge(HrTblEmployee);
			}
			else
			{
				try {
					//HrTblEmployee.setSerEmployeeId(Integer.parseInt(HrTblEmployee.getTxtEmployeeCode()));
				} 
				catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "format";
				}
				catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "format";
				}
			entityManager.persist(HrTblEmployee);
			}
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			 e.printStackTrace();
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteEmployees(List<String> EmployeesId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serEmployeeId : EmployeesId) {
				HrTblEmployee Employee = entityManager.find(HrTblEmployee.class, Integer.parseInt(serEmployeeId));
				if (Employee != null) {
					Employee.setBlIsDeleted(true);
					Employee.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
					Employee.setSerModifiedUser(commonService.getCurrentLoggedInUser());
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
	public String updateEmployee(HrTblEmployee hrTblEmployee) {
		EntityManager entityManager = getEntityManager();
		try {
			System.err.println("updateEmployee Is called ####################### ");
//			createJobHistory(hrTblEmployee);
//			updateAttendance(hrTblEmployee);
			entityManager.getTransaction().begin();
			hrTblEmployee.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
			hrTblEmployee.setSerModifiedUser(commonService.getCurrentLoggedInUser());
			
			try {
//				hrTblEmployee.setSerEmployeeId(Integer.parseInt(hrTblEmployee.getTxtEmployeeCode()));
			} 
			catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "format";
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "format";
			}
			entityManager.merge(hrTblEmployee);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	private String updateAttendance(HrTblEmployee hrTblEmployee) {
		
// 		EntityManager entityManager= getEntityManager();
//		entityManager.getTransaction().begin();
//		
////		SimpleDateFormatter formatter = new SimpleDateFormatter("yyyy-MM-dd");
////		Date date =  formatter.parse( formatter.format(commonService.getCurrentTimeStamp_new()));
//		Date date =new Date();
//		String query = "FROM HrTblAttendanceInfo info where info.hrTblEmployee.serEmployeeId= " + hrTblEmployee.getSerEmployeeId() + " and info.dteDate = '" +date+"'" ;
//		
//		
//		HrTblAttendanceInfo  attendance = (HrTblAttendanceInfo) entityManager.createQuery(query).getSingleResult();
//		query = " From HrTblJob where  serEmployeeId " + hrTblEmployee.getSerEmployeeId();
//		HrTblJob job = (HrTblJob) entityManager.createQuery(query).getSingleResult();
//		
//		if(attendance == null) {
//			return "no record found";
//		}
//		
//		if(job.getRate() == attendance.getNumRate()) {
//			return "no need to update";
//		}else {
//			attendance.setNumRate(job.getRate());
//			attendance.setHrTblJob(job);
//		}
//		
//		attendance.setDteCreateddate(commonService.getCurrentTimeStamp_new());
//		attendance.setDteCreateddate(commonService.getCurrentTimeStamp_new());
//		entityManager.merge(attendance);
//		entityManager.getTransaction().commit();
//		entityManager.close();
		 
		return "success";
	} 
	private String createJobHistory(HrTblEmployee employeeCurrent){
		//amir is here
//		EntityManager entityManager= getEntityManager();
//		entityManager.getTransaction().begin();
//		String query = "FROM HrTblEmployee where serEmployeeId= " + employeeCurrent.getSerEmployeeId();
//		HrTblEmployee employeeDB = (HrTblEmployee) entityManager.createQuery(query).getSingleResult();
//		HrTblEmployeeJobHistory history = new HrTblEmployeeJobHistory();
//		
//		if(employeeDB.getJob() == null && employeeCurrent.getJob()!=null) {
//			history.setSerJobId(employeeCurrent.getJob().getSerJobId());
//			history.setSerEmployeeId(employeeCurrent.getSerEmployeeId());
//		}
//		
//		if(employeeCurrent.getJob() != null && employeeDB.getJob() != null) {
//			if(employeeCurrent.getJob().getSerJobId() != employeeDB.getJob().getSerJobId() ) {
//				history.setSerJobId(employeeCurrent.getJob().getSerJobId());
//				history.setSerEmployeeId(employeeCurrent.getSerEmployeeId());
//			}
//		}
//		history.setDteCreateddate(commonService.getCurrentTimeStamp_new());
//		history.setDteCreateddate(commonService.getCurrentTimeStamp_new());
//		entityManager.persist(history);
//		entityManager.getTransaction().commit();
//		entityManager.close();
		return "success";
 	}
	
private void updateAttendanceInfo(){
	
}

	@Override
	public String generateEmployeeNo(String type) {
		// int EmployeeNo;
		String EmployeeType = type;
		// String EmployeeCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (EmployeeType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtEmployeeCode) from HrTblEmployee ")
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

				String zoneCode = (String) entityManager.createQuery("select MAX(txtEmployeeCode) from HrTblEmployee ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "EMP-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(4));

				ord_no1 = ord_no1 + 1;
				String code = "EMP-1";
				if (ord_no1 < 10)
					code = "EMP-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "EMP-0" + ord_no1;
				else
					code = "EMP-" + ord_no1;
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

	public String getEmployeeById(String EmployeeId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblEmployee where txtEmployeeCode='" + EmployeeId + "'";

			List<HrTblEmployee> Employee = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Employee.size() > 0) {
				return String.valueOf(Employee.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public HrTblEmployee getEmployeeByPK(int EmployeeId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM HrTblEmployee where serEmployeeId=" + EmployeeId ;

			HrTblEmployee Employee = (HrTblEmployee)entityManager.createQuery(query).getSingleResult();

			entityManager.close();
			
			return Employee;

		}  
		catch (NoResultException e) {
			log.error("Employee not found");
			return null;
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<HrTblEmployee> searchEmployee(HrTblEmployee Employee) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from HrTblEmployee  emp where 1=1 ";
	    if(Employee.getTxtEmployeeCode() != null)
	    {
	    	query+=" and upper(emp.txtEmployeeCode) like"+" upper('"+Employee.getTxtEmployeeCode()+"')"+" ";
	    }
	    if(Employee.getTxtEmployeeName() !=null){
	    	query+=" and ( upper(emp.txtEmployeeName) like"+" upper('"+Employee.getTxtEmployeeName()+"')"+"  ";
	    }
	    
	    if(Employee.getTxtMobileNo() !=null && Employee.getTxtEmployeeName() !=null){
	    	query+=" or upper(emp.txtMobileNo) like"+" upper('"+Employee.getTxtMobileNo()+"')"+"  ";
	    }
	    else if(Employee.getTxtMobileNo() !=null)
	    {
	    	query+=" and upper(emp.txtMobileNo) like"+" upper('"+Employee.getTxtMobileNo()+"')"+"  ";
	    }
	     if(Employee.getTxtEmployeeName() !=null){
	    	query+=" ) ";
	    }
	    	
	    
	    if(Employee.getTxtCnic() !=null){
	    	query+=" and upper(emp.txtCnic) like"+" upper('"+Employee.getTxtCnic()+"')"+"  ";
	    }
	    
	    /*if(Employee.getTxtEmail() !=null){
	    	query+=" and upper(Employee.txtEmail) like"+" upper('"+Employee.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Employee.getSerEmployeeId() !=null){
	    	query+=" and emp.serEmployeeId ="+" "+Employee.getSerEmployeeId()+""+"  ";
	    }
	    
	    if(Employee.getCfgTblCity() !=null && Employee.getCfgTblCity().getSerCityId()!=null && Employee.getCfgTblCity().getSerCityId() >0){
	    	query+=" and emp.cfgTblCity.serCityId ="+" "+Employee.getCfgTblCity().getSerCityId()+""+"  ";
	    }
	  
	    query+=" order by emp.serEmployeeId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<HrTblEmployee> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
