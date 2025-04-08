package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.util.Map;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.dal.dao.IDBDAO;

import org.springframework.jdbc.core.JdbcTemplate;

@Repository
public class DBDAO implements IDBDAO {

	/*
	 * @Autowired private EntityManagerFactory entityManagerFactory;
	 */

	@Autowired
	private ICommonService commonService;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private LoginDAO loginDao;

	private JdbcTemplate jdbcTemplateObject;

	private static final Logger log = LoggerFactory.getLogger(DBDAO.class);

	public DBDAO() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * private EntityManager getEntityManager() { return
	 * entityManagerFactory.createEntityManager(); }
	 */

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");

	/*
	 * String date = simpleDateFormat.format(new Date()); System.out.println(date);
	 */

	public List<Map<String, Object>> getJobCardDAO(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
//		if (dto.getSer_invoice_id() > 0)

			SQL = "select count(ser_work_order_id)as count ,category.txt_name from sls_tbl_work_order  \r\n"
					+ "LEFT JOIN cfg_tbl_jobcategory category ON category.ser_jobcategory_id = sls_tbl_work_order.ser_jobcategory_id where 1=1  ";

		SQL2 = "  group by category.txt_name ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	
	public List<Map<String, Object>> getJobCardSummaryDAO(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "select count(ser_work_order_id) as total,txt_status from sls_tbl_work_order where 1=1  ";

		SQL2 = "  group by txt_status ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	
	public List<Map<String, Object>> getJobCardSummaryMonthwise(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "SELECT  count(ser_work_order_id) as total, TO_CHAR (dte_Date, 'Month')||' '|| EXTRACT (year FROM dte_Date) as month FROM sls_tbl_work_order where 1=1  "
					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   GROUP BY TO_CHAR(dte_Date, 'Month'), EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date)\r\n" + 
				"\r\n" + 
				"	order by  EXTRACT (year FROM dte_Date),EXTRACT (MONTH FROM dte_Date) ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	/**
	 * 
	 * 
	 * Complaints
	 * 
	 */
	
	
	public List<Map<String, Object>> getComplaintsSummaryMonthwise(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "SELECT  count(ser_complaint_id) as total, TO_CHAR (dte_Date, 'Month')||' '|| EXTRACT (year FROM dte_Date) as month FROM sls_tbl_complaint where 1=1  ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   GROUP BY TO_CHAR(dte_Date, 'Month'), EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date)\r\n" + 
				"\r\n" + 
				"	order by  EXTRACT (year FROM dte_Date),EXTRACT (MONTH FROM dte_Date) ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	
	public List<Map<String, Object>> getComplaintsSummaryDepartmentwise(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "select count(ser_complaint_id) as total,txt_department as name FROM sls_tbl_complaint where 1=1  ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   group by txt_department ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	public List<Map<String, Object>> getComplaintsSummaryStatuswise(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "select count(ser_complaint_id) as total,txt_status as name FROM sls_tbl_complaint where 1=1  ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   group by txt_status ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	public List<Map<String, Object>> getComplaintsSummaryDealerwise(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "select count(ser_complaint_id) as total,customer.txt_customer_name as name\r\n" + 
					" from sls_tbl_complaint\r\n" + 
					"left join cfg_tbl_customer customer on customer.ser_customer_id =sls_tbl_complaint.ser_customer_id where 1=1  ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   group by customer.txt_customer_name ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	/**
	 * 
	 * 
	 * Claims
	 * 
	 * 
	 * 
	 */
	
	public List<Map<String, Object>> getClaimSummary(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "select sum(num_amount_after_tax) as total ,sum(c.num_amount_before_tax) as tax ,txt_customer_name as name\r\n" + 
					",TO_CHAR(dte_Date, 'Month') as month, EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date)\r\n" + 
					"from sls_tbl_claim c\r\n" + 
					"LEFT JOIN cfg_tbl_customer customer ON customer.ser_customer_id = c.ser_dealer_id where 1=1  ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   group by txt_customer_name,TO_CHAR(dte_Date, 'Month'), EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date)\r\n" + 
				"  order by EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date) ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	public List<Map<String, Object>> getClaimSummarybyMonth(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = " select sum(num_amount_after_tax) as total ,sum(c.num_amount_before_tax) as tax \r\n" + 
					",TO_CHAR(dte_Date, 'Month')||' '||EXTRACT (year FROM dte_Date) as name, EXTRACT (MONTH FROM dte_Date)\r\n" + 
					"from sls_tbl_claim c\r\n" + 
					"where num_level=6   ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   group by TO_CHAR(dte_Date, 'Month'), EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date)\r\n" + 
				" order by EXTRACT (MONTH FROM dte_Date), EXTRACT (year FROM dte_Date) ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	public List<Map<String, Object>> getClaimSummarybyApproval(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
	
			SQL = "select count(ser_claim_id) as count,num_level as level,sum(num_amount_after_tax) as total \r\n" + 
					" from sls_tbl_claim where 1=1  ";
//					+ " and dte_date >= '2023-01-01' ";

		SQL2 = "   group by num_level order by  num_level ";

		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id = " + dto.getSer_dealer_id() + "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from + "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  dte_date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	

}
