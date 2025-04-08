package com.bezkoder.spring.login.sa.dal.daoimpl;/*package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.persistence.*;;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;


import com.bezkoder.spring.login.sa.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.IReportDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblLedgerDAO;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblLedger;

@Repository
public class ReportDAOImpl //implements  IReportDAO 
{

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(ReportDAOImpl.class);

	public ReportDAOImpl() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	private JdbcTemplate jdbcTemplateObject;
	
	private DataSource dataSource;

	 @Autowired
	 public void setDataSource(DataSource dataSource) {
	    this.dataSource = dataSource;
	 }
	   
	  DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	  DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");
	  
	
	 
	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getUploadLedgersReport(String city,String driver,String customer,String date_from,String date_to) {
		 this.dataSource = dataSource;
	      this.jdbcTemplateObject = new JdbcTemplate(dataSource);
		
//		String query = "from CitTableJobCard  jobcardtbl WHERE dteJobCardDate!=null";
	      
//	      String SQL = "select *,subject.s_NAME from Student left outer join subject on Student.id=subject.student_id";
//	      List <Student> students = jdbcTemplateObject.query(SQL, new StudentMapper());
		
	      ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
	      System.out.println("------"+rb.getString("upload_all"));
	      String SQL=rb.getString("upload_all");
	      StringBuffer WhereClause=new StringBuffer();;
	      
	      
	      WhereClause.append(" and num_paid_amount is null ");
	      
	      if(city !=null && city.trim().length()>0 && !(city.equalsIgnoreCase("null")) && !(city.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_city_id="+city);
	      }
	      
	      if(driver !=null && driver.trim().length()>0  && !(driver.equalsIgnoreCase("null")) && !(driver.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_employee_id="+driver);
	      }
	      
	      if(customer !=null && customer.trim().length()>0 && !(customer.equalsIgnoreCase("null")) && !(customer.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_customer_id="+customer );
	      }
	   	      if(date_from !=null && date_from.trim().length()>0)
	      {
	    	  try {
				WhereClause.append(" and sls_tbl_ledger.dte_date_from = '"+DATE_FORMATDB.format(DATE_FORMAT.parse(date_from))+"'");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      
	      if(date_to !=null && date_to.trim().length()>0)
	      {
	    	  try {
				WhereClause.append(" and sls_tbl_ledger.dte_date_to = '"+DATE_FORMATDB.format(DATE_FORMAT.parse(date_to))+"'");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      WhereClause.append(" order by sls_tbl_ledger.ser_ledger_id ");
	  		 System.out.println("report  :-"+SQL+WhereClause);
		 List<Map<String, Object>> rs=jdbcTemplateObject.queryForList(SQL+WhereClause);
		 
		 return rs;

		
	}
	
	
    
	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getPaymentReport(String city,String driver,String customer,String date_from,String date_to) {
		 this.dataSource = dataSource;
	      this.jdbcTemplateObject = new JdbcTemplate(dataSource);
		
//		String query = "from CitTableJobCard  jobcardtbl WHERE dteJobCardDate!=null";
	      
//	      String SQL = "select *,subject.s_NAME from Student left outer join subject on Student.id=subject.student_id";
//	      List <Student> students = jdbcTemplateObject.query(SQL, new StudentMapper());
		
	      ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
	      System.out.println("------"+rb.getString("upload_all"));
	      String SQL=rb.getString("upload_all");
	      StringBuffer WhereClause=new StringBuffer();;
	      
	      WhereClause.append(" and num_paid_amount >0 ");
	      if(city !=null && city.trim().length()>0 && !(city.equalsIgnoreCase("null")) && !(city.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_city_id="+city);
	      }
	      
	      if(driver !=null && driver.trim().length()>0  && !(driver.equalsIgnoreCase("null")) && !(driver.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_employee_id="+driver);
	      }
	      
	      if(customer !=null && customer.trim().length()>0 && !(customer.equalsIgnoreCase("null")) && !(customer.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_customer_id="+customer );
	      }
	   	      if(date_from !=null && date_from.trim().length()>0)
	      {
	    	  try {
				WhereClause.append(" and sls_tbl_ledger.dte_date >= '"+DATE_FORMATDB.format(DATE_FORMAT.parse(date_from))+"'");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      
	      if(date_to !=null && date_to.trim().length()>0)
	      {
	    	  try {
//	    		  searchDTO.setDte_start_date(DATE_FORMATDB.format(DATE_FORMAT.parse(date_to.trim())));
	    		  
				WhereClause.append(" and sls_tbl_ledger.dte_date <= '"+DATE_FORMATDB.format(DATE_FORMAT.parse(date_to))+"'");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      WhereClause.append(" order by sls_tbl_ledger.ser_supplier_id,hr_tbl_employee.txt_employee_name ");
	  		 System.out.println("report  :-"+SQL+WhereClause);
		 List<Map<String, Object>> rs=jdbcTemplateObject.queryForList(SQL+WhereClause);
		 
		 return rs;

		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Map<String, Object>> getLedgerReport(String city,String driver,String customer,String date_from,String date_to) {
		 this.dataSource = dataSource;
	      this.jdbcTemplateObject = new JdbcTemplate(dataSource);
		
//		String query = "from CitTableJobCard  jobcardtbl WHERE dteJobCardDate!=null";
	      
//	      String SQL = "select *,subject.s_NAME from Student left outer join subject on Student.id=subject.student_id";
//	      List <Student> students = jdbcTemplateObject.query(SQL, new StudentMapper());
		
	      ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
	      System.out.println("------"+rb.getString("upload_all"));
	      String SQL=rb.getString("upload_all");
	      StringBuffer WhereClause=new StringBuffer();;
//	      WhereClause.append(" and num_paid_amount is not null ");
	      if(city !=null && city.trim().length()>0 && !(city.equalsIgnoreCase("null")) && !(city.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_city_id="+city);
	      }
	      
	      if(driver !=null && driver.trim().length()>0  && !(driver.equalsIgnoreCase("null")) && !(driver.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_employee_id="+driver);
	      }
	      
	      if(customer !=null && customer.trim().length()>0 && !(customer.equalsIgnoreCase("null")) && !(customer.equalsIgnoreCase("undefined")))
	      {
	    	  WhereClause.append(" and sls_tbl_ledger.ser_customer_id="+customer );
	      }
	   	      if(date_from !=null && date_from.trim().length()>0)
	      {
	    	  try {
				WhereClause.append(" and sls_tbl_ledger.dte_date >= '"+DATE_FORMATDB.format(DATE_FORMAT.parse(date_from))+"'");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      
	      if(date_to !=null && date_to.trim().length()>0)
	      {
	    	  try {
				WhereClause.append(" and sls_tbl_ledger.dte_date <= '"+DATE_FORMATDB.format(DATE_FORMAT.parse(date_to))+"'");
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      }
	      
	      WhereClause.append(" order by sls_tbl_ledger.ser_customer_id,sls_tbl_ledger.ser_employee_id,sls_tbl_ledger.ser_ledger_id ");
	  		 System.out.println("report  :-"+SQL+WhereClause);
		 List<Map<String, Object>> rs=jdbcTemplateObject.queryForList(SQL+WhereClause);
		 
		 return rs;

		
	}
}
*/