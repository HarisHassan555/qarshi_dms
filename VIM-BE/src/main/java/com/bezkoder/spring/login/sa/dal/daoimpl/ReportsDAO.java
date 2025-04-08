package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
import com.bezkoder.spring.login.sa.dal.dao.IReportDAO;

import org.springframework.jdbc.core.JdbcTemplate;

@Repository
public class ReportsDAO implements IReportDAO {

/*	@Autowired
	private EntityManagerFactory entityManagerFactory;*/

	@Autowired
	private ICommonService commonService;

	@Autowired
	private DataSource dataSource;
	
	@Autowired
	private LoginDAO loginDao;
	
	private JdbcTemplate jdbcTemplateObject;

	private static final Logger log = LoggerFactory.getLogger(ReportsDAO.class);

	public ReportsDAO() {
		// TODO Auto-generated constructor stub
	}

/*	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}*/

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
	

	/*
	 * String date = simpleDateFormat.format(new Date()); System.out.println(date);
	 */
	
	@Override
	public List<Map<String, Object>> getAttendenceReport(ReportDTO dto) {

		
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		

			SQL = " SELECT * from view_attendance where 1=1  ";

		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  dte_date >= '" + dt_from
						+ "'";

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
		
		
		if (dto.getTxt_employee_code()!=null && dto.getTxt_employee_code().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_employee_code  ilike '" + dto.getTxt_employee_code() + "'";
		
		if (dto.getTxt_employee_name()!=null && dto.getTxt_employee_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_employee_name  ilike '" + dto.getTxt_employee_name() + "%'";
		
		if (dto.getTxt_department_name()!=null && dto.getTxt_department_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_department_name  ilike '" + dto.getTxt_department_name() + "%'";
		
		if (dto.getTxt_designation_name()!=null && dto.getTxt_designation_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_designation_name  ilike '" + dto.getTxt_designation_name() + "%'";
		
		if (dto.getTxtSite()!=null && dto.getTxtSite().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_site_name  ilike '" + dto.getTxtSite() + "%'";
		
		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0 && dto.getTxt_product_name().trim().equalsIgnoreCase("true"))
			SQL = String.valueOf(SQL) + " and num_working_hours < 0.1 ";
		
		

		 if (dto.getTxtType()!=null && dto.getTxtType().trim().equalsIgnoreCase("emp"))
				SQL2 = String.valueOf(SQL2) + "  order by txt_employee_name,dte_date";
		 
		 else if (dto.getTxtType()!=null && dto.getTxtType().trim().equalsIgnoreCase("Dept"))
				SQL2 = String.valueOf(SQL2) + "  order by txt_department_name,dte_date,tim_incoming_time:: time";
		 
		 else if (dto.getTxtType()!=null && dto.getTxtType().trim().equalsIgnoreCase("Desig"))
				SQL2 = String.valueOf(SQL2) + "  order by txt_designation_name,dte_date,tim_incoming_time:: time";
		 
		 else 
			 SQL2 = String.valueOf(SQL2) + "  order by dte_date,tim_incoming_time:: time";
//			SQL2 = String.valueOf(SQL2) + "  order by txt_designation_name,dte_date,txt_employee_name";
		
//	if((dto.getTxtType()!=null && dto.getTxtType().trim().equalsIgnoreCase("dept")))
		
		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	

	public List<Map<String, Object>> getSaleDateWise(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT date(pos_order.date_order + interval '1' HOUR * 5) AS date,\r\n"
					+ "to_char(date(pos_order.date_order + interval '1' HOUR * 5), 'Month') AS Month,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) AS sale_before_disc,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (pos_order_line.discount / 100::numeric)), '0'::numeric) AS total_discount,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) AS sale_after_disc,\r\n"
					+ "     COALESCE(sum( CASE WHEN (pos_order_line.qty::double precision * pos_order_line.cost_at_sale_time) = 0::double precision THEN (pos_order_line.qty * (( SELECT product_price_history.cost      FROM product_price_history\r\n"
					+ "              WHERE product_price_history.product_id = pos_order_line.product_id\r\n"
					+ "              ORDER BY product_price_history.product_id, product_price_history.id DESC\r\n"
					+ "             LIMIT 1)))::double precision\r\n"
					+ "            ELSE pos_order_line.qty::double precision * pos_order_line.cost_at_sale_time\r\n"
					+ "        END), '0'::double precision) AS costt\r\n" + "\r\n" + "   FROM pos_order_line\r\n"
					+ "     JOIN pos_order ON pos_order.id = pos_order_line.order_id\r\n"
					+ "     JOIN product_product ON pos_order_line.product_id = product_product.id\r\n"
					+ "     JOIN product_template ON product_template.id = product_product.product_tmpl_id\r\n"
					+ "      LEFT JOIN product_category  ON product_template.categ_id = product_category.id where 1=1  ";

		SQL2 = "  group by date(pos_order.date_order + interval '1' HOUR * 5)\r\n"
				+ "  order by  date(pos_order.date_order + interval '1' HOUR * 5)";
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	public List<Map<String, Object>> getSaleDateWiseWOProfit(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT date(pos_order.date_order + interval '1' HOUR * 5) AS date,\r\n"
					+ "to_char(date(pos_order.date_order + interval '1' HOUR * 5), 'Month') AS Month,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) AS sale_before_disc,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (pos_order_line.discount / 100::numeric)), '0'::numeric) AS total_discount,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) AS sale_after_disc,\r\n"
					+ "    0.0 AS costt\r\n" + "\r\n" + "   FROM pos_order_line\r\n"
					+ "     JOIN pos_order ON pos_order.id = pos_order_line.order_id\r\n"
					+ "     JOIN product_product ON pos_order_line.product_id = product_product.id\r\n"
					+ "     JOIN product_template ON product_template.id = product_product.product_tmpl_id\r\n"
					+ "      LEFT JOIN product_category  ON product_template.categ_id = product_category.id where 1=1  ";

		SQL2 = "  group by date(pos_order.date_order + interval '1' HOUR * 5)\r\n"
				+ "  order by  date(pos_order.date_order + interval '1' HOUR * 5)";
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	public List<Map<String, Object>> getSaleCategoryWise(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT product_category.complete_name as name,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) AS sale_before_disc,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (pos_order_line.discount / 100::numeric)), '0'::numeric) AS total_discount,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) AS sale_after_disc,\r\n"
					+ "     COALESCE(sum( CASE WHEN (pos_order_line.qty::double precision * pos_order_line.cost_at_sale_time) = 0::double precision THEN (pos_order_line.qty * (( SELECT product_price_history.cost      FROM product_price_history\r\n"
					+ "              WHERE product_price_history.product_id = pos_order_line.product_id\r\n"
					+ "              ORDER BY product_price_history.product_id, product_price_history.id DESC\r\n"
					+ "             LIMIT 1)))::double precision\r\n"
					+ "            ELSE pos_order_line.qty::double precision * pos_order_line.cost_at_sale_time\r\n"
					+ "        END), '0'::double precision) AS costt\r\n" + "\r\n" + "   FROM pos_order_line\r\n"
					+ "     JOIN pos_order ON pos_order.id = pos_order_line.order_id\r\n"
					+ "     JOIN product_product ON pos_order_line.product_id = product_product.id\r\n"
					+ "     JOIN product_template ON product_template.id = product_product.product_tmpl_id\r\n"
					+ "      LEFT JOIN product_category  ON product_template.categ_id = product_category.id where 1=1  ";

		SQL2 = "  group by product_category.complete_name \r\n"
				+ "  order by  COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) DESC ";
		
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product_category.complete_name  ilike '" + dto.getCategory() + "%'";
		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	public List<Map<String, Object>> getSaleCategoryWiseWOProfit(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT product_category.complete_name as name,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) AS sale_before_disc,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (pos_order_line.discount / 100::numeric)), '0'::numeric) AS total_discount,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) AS sale_after_disc,\r\n"
					+ "     0.0 AS costt\r\n" + "\r\n" + "   FROM pos_order_line\r\n"
					+ "     JOIN pos_order ON pos_order.id = pos_order_line.order_id\r\n"
					+ "     JOIN product_product ON pos_order_line.product_id = product_product.id\r\n"
					+ "     JOIN product_template ON product_template.id = product_product.product_tmpl_id\r\n"
					+ "      LEFT JOIN product_category  ON product_template.categ_id = product_category.id where 1=1  ";

		SQL2 = "  group by product_category.complete_name \r\n"
				+ "  order by  COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) DESC ";
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product_category.complete_name  ilike '" + dto.getCategory() + "%'";
		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	public List<Map<String, Object>> getTopSellingProductQtyWise(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT product_category.complete_name,product_template.name,COALESCE(sum(pos_order_line.qty)) as qty ,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) AS sale_before_disc,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (pos_order_line.discount / 100::numeric)), '0'::numeric) AS total_discount,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) AS sale_after_disc,\r\n"
					+ "     0.0 AS costt\r\n" + "\r\n" + "   FROM pos_order_line\r\n"
					+ "     JOIN pos_order ON pos_order.id = pos_order_line.order_id\r\n"
					+ "     JOIN product_product ON pos_order_line.product_id = product_product.id\r\n"
					+ "     JOIN product_template ON product_template.id = product_product.product_tmpl_id\r\n"
					+ "      LEFT JOIN product_category  ON product_template.categ_id = product_category.id where 1=1  ";

		SQL2 = "  group by product_category.complete_name,product_template.name \r\n"
				+ "  order by  COALESCE(sum(pos_order_line.qty)) DESC ";
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product_category.complete_name  ilike '" + dto.getCategory() + "%'";
		
		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product_template.name  ilike '" + dto.getTxt_product_name() + "%'";
		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	public List<Map<String, Object>> getTopSellingProductAmountWise(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT product_category.complete_name,product_template.name,COALESCE(sum(pos_order_line.qty)) as qty ,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit), '0'::numeric) AS sale_before_disc,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (pos_order_line.discount / 100::numeric)), '0'::numeric) AS total_discount,\r\n"
					+ "    COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) AS sale_after_disc,\r\n"
					+ "     0.0 AS costt\r\n" + "\r\n" + "   FROM pos_order_line\r\n"
					+ "     JOIN pos_order ON pos_order.id = pos_order_line.order_id\r\n"
					+ "     JOIN product_product ON pos_order_line.product_id = product_product.id\r\n"
					+ "     JOIN product_template ON product_template.id = product_product.product_tmpl_id\r\n"
					+ "      LEFT JOIN product_category  ON product_template.categ_id = product_category.id where 1=1  ";

		SQL2 = "  group by product_category.complete_name,product_template.name \r\n"
				+ "  order by  COALESCE(sum(pos_order_line.qty * pos_order_line.price_unit * (100::numeric - pos_order_line.discount) / 100::numeric), '0'::numeric) DESC ";
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product_category.complete_name  ilike '" + dto.getCategory() + "%'";
		
		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product_template.name  ilike '" + dto.getTxt_product_name() + "%'";
		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date(pos_order.date_order + interval '1' HOUR * 5) <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	
	public List<Map<String, Object>> getStockReportProductWise(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT * from view_stock where 1=1  ";

		
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and category  ilike '%" + dto.getCategory() + "%'";
		
		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and product  ilike '" + dto.getTxt_product_name() + "%'";
		
		if(dto.getTxtStatus()!=null && dto.getTxtStatus().trim().equalsIgnoreCase("zero"))
			SQL = String.valueOf(SQL) + " and  stock <= 0";
		else
			SQL = String.valueOf(SQL) + " and  stock > 0";

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	public List<Map<String, Object>> getStockReportCategoryWise(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT * from view_stock_category where 1=1  ";

		
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and category  ilike '%" + dto.getCategory() + "%'";
				
		if(dto.getTxtStatus()!=null && dto.getTxtStatus().trim().equalsIgnoreCase("zero"))
			SQL = String.valueOf(SQL) + " and  stock <= 0";
		else
			SQL = String.valueOf(SQL) + " and  stock > 0";

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	
	public List<Map<String, Object>> getSaleProductWiseWOProfit(ReportDTO dto)
	{
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		// ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		String SQL = "";
		String SQL2 = "";
		if (dto.getSer_invoice_id() > 0)

			SQL = " SELECT * from sale_product_wise where 1=1  ";

		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				String dt_from = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));

				SQL = String.valueOf(SQL) + " and  date >= '" + dt_from
						+ "'";

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {

				String dt = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())); // Start date

				SQL = String.valueOf(SQL) + " and  date <= '" + dt + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		if (dto.getCategory()!=null && dto.getCategory().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and category  ilike '%" + dto.getCategory() + "%'";
		
		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and name  ilike '" + dto.getTxt_product_name() + "%'";

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2);
		return rs;
	}
	
	@Override
	public List<Map<String, Object>> getStockReportatProcess(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("stock_all_process"));
		String SQL = rb.getString("stock_all_process");

		if (dto.getSer_process_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_process_id=" + dto.getSer_process_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id=" + dto.getSer_product_category_id();

		if (dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_product_name  ilike '" + dto.getTxt_product_name() + "%'";

		SQL = String.valueOf(SQL) + " and num_balance >0  order by txt_product_category_name,txt_product_name";
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	 DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	  DateFormat DATE_FORMATDB = new SimpleDateFormat("yyyy-MM-dd");
	
	  
	@Override
	public List<Map<String, Object>> getStockLedgerReportatProcess(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("stock_ledger_all_process"));
		String SQL = rb.getString("stock_ledger_all_process");

		if (dto.getSer_process_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_process_id=" + dto.getSer_process_id();

		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id=" + dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id=" + dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id=" + dto.getSer_product_quality_id();

		if (dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_product_name  ilike '" + dto.getTxt_product_name() + "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getSaleOrderReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("view_sale_order"));
		String SQL="";
		if(dto.getSer_proforma_invoice_id()>0)

			 SQL = rb.getString("view_proforma_invoice");
		else
		 SQL = rb.getString("view_sale_order");
		
		
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getTxtUserName().equalsIgnoreCase("umair")) {
			
			SQL = String.valueOf(SQL) + " and ( serGroupId =  "+ user.getSerGroupId() +"  or serProductId in (904,905,906,907,908,909)) " ;
			
//			query += " and ( serGroupId = " + user.getSerGroupId() + " or serProductId in (904,905,906,907,908,909)) ";

		}
		else if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			
			SQL = String.valueOf(SQL) + " and  serGroupId =  "+ user.getSerGroupId() +" " ;
			
//			query += " and SaleOrder.serGroupId = " + user.getSerGroupId() + " ";

		}
		

		if (dto.getSer_sale_order_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_sale_order_id=" + dto.getSer_sale_order_id();
		
		if (dto.getSer_proforma_invoice_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_proforma_invoice_id=" + dto.getSer_proforma_invoice_id();
		
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id="
					+ dto.getSer_product_quality_id();
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();

		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "'";
		
		
		if (dto.getTxtDealer()!=null && dto.getTxtDealer().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and dealer  ilike '" + dto.getTxtDealer()
					+ "%'";
		
		
		if (dto.getTxtCustomer()!=null && dto.getTxtCustomer().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_customer_name  ilike '" + dto.getTxtCustomer()
					+ "%'";
		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  so_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  so_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
//		 and txt_status = 'APPROVED'

		SQL = String.valueOf(SQL) +" and  ser_created_user_id > 0 "
				+ " ";
//		CfgTblUser cfgTblUser = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
//		if (cfgTblUser.getCfgTblCustomer() != null) {
//			if (cfgTblUser.getCfgTblCustomer().getBlIsDealer() != null
//					&& cfgTblUser.getCfgTblCustomer().getBlIsDealer()) {
//				// query+=" and SaleOrder.cfgTblDealer.serCustomerId ="+"
//				// "+cfgTblUser.getCfgTblCustomer().getSerCustomerId()+""+" ";
//				
//				SQL += String.valueOf(SQL) + " and dealerid in"
//						+ " (select ser_customer_id from cfg_tbl_customer customer  where customer.ser_customer_id="
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
//						+ "  or customer.ser_parent_customer_id =   "
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()+
//						 ")";
//			} else {
//				SQL += String.valueOf(SQL) +  " and customerid =" + " "
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + "" + "  ";
//
//			}
//
//			/*	SQL += String.valueOf(SQL) + " and SaleOrder.cfgTblDealer.serCustomerId in"
//						+ " (select serCustomerId from CfgTblCustomer customer  where customer.serCustomerId="
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
//						+ "  or customer.cfgTblGroupCustomer.serCustomerId=   "
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId()
//						+ " or customer.cfgTblCustomer.serCustomerId=   "
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + ")";
//			} else {
//				query += " and SaleOrder.cfgTblCustomer.serCustomerId =" + " "
//						+ cfgTblUser.getCfgTblCustomer().getSerCustomerId() + "" + "  ";
//
//			}*/
//		}

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	@Override
	public List<Map<String, Object>> getDCReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("view_delivery_challan"));
		String SQL = rb.getString("view_delivery_challan");

		if (dto.getSer_issue_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_issue_id=" + dto.getSer_issue_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	

	
	
	@Override
	public List<Map<String, Object>> getTranferReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("view_transfer_stock"));
		String SQL = rb.getString("view_transfer_stock");

		if (dto.getSer_poduction_detail_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_poduction_detail_id=" + dto.getSer_poduction_detail_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	////////////////////////////////////////////////////RPT Dispatch Report Customer Wise/////////////////
	
	@Override
	public List<Map<String, Object>> getDisptachReportCustomerWise(ReportDTO dto) {

		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

//		String SQL = "  SELECT cfg_tbl_customer.txt_customer_code,cfg_tbl_customer.txt_customer_name,cfg_tbl_customer.txt_gst_number,\r\n"
//				+ "    cfg_tbl_customer.txt_billing_address,cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,\r\n"
//				+ "    cfg_tbl_product.txt_master_pack,cfg_tbl_product.txt_price_unit,cfg_tbl_product.num_units_in_master_pack,\r\n"
//				+ "    cfg_tbl_product.num_pieces_in_master_pack,cfg_tbl_product_quality.txt_product_quality_name,cfg_tbl_product_design.txt_product_design_name,\r\n"
//				+ "    cfg_tbl_product_category.txt_product_category_name,\r\n"
//				+ "    sum(inv_tbl_issue_detail.num_quantity) as num_quantity,\r\n"
//				+ "    sum(inv_tbl_issue_detail.num_qty_in_bags) as num_qty_in_bags,\r\n"
//				+ "    sum(sls_tbl_so_detail.num_item_price*inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack) AS amount,\r\n"
//				+ "    sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack) AS no_of_units,\r\n"
//				+ "    sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight) AS toonage,\r\n"
//				+ "    (sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight)/1000000) AS tons,\r\n"
//				+ "   (ROUND(sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight)/sum(inv_tbl_issue_detail.num_quantity),2)) AS unit_wt,\r\n"
//				+ "    sum(cfg_tbl_product.num_pieces_in_master_pack * inv_tbl_issue_detail.num_quantity) AS pcs\r\n"
//				+ "    ,sum(sls_tbl_so_detail.num_item_price*inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack)/(sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight)/1000000) as rpt\r\n"
//				+ " FROM inv_tbl_issue_detail \r\n"
//				+ "     LEFT JOIN inv_tbl_issue ON inv_tbl_issue_detail.ser_issue_id = inv_tbl_issue.ser_issue_id\r\n"
//				+ "     LEFT JOIN sls_tbl_sale_order ON sls_tbl_sale_order.ser_sale_order_id = inv_tbl_issue.ser_sale_order_id\r\n"
//				+ "     LEFT JOIN sls_tbl_so_detail ON sls_tbl_so_detail.ser_so_detail_id = inv_tbl_issue_detail.ser_so_detail_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_product ON cfg_tbl_product.ser_product_id = inv_tbl_issue_detail.ser_product_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_product_category ON cfg_tbl_product_category.ser_product_category_id = cfg_tbl_product.ser_product_category_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_customer ON cfg_tbl_customer.ser_customer_id = sls_tbl_sale_order.ser_customer_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_brand ON cfg_tbl_product.ser_brand_id = cfg_tbl_brand.ser_brand_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_product_design ON inv_tbl_issue_detail.ser_product_design_id = cfg_tbl_product_design.ser_product_design_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_product_quality ON cfg_tbl_product_quality.ser_product_quality_id = inv_tbl_issue_detail.ser_product_quality_id\r\n"
//				+ "     LEFT JOIN cfg_tbl_supplier ON cfg_tbl_supplier.ser_supplier_id = inv_tbl_issue.ser_supplier_id \r\n"
//				+ "     LEFT JOIN cfg_tbl_city ON cfg_tbl_city.ser_city_id = cfg_tbl_customer.ser_city_id   where 1=1 ";
		
		String SQL = "  SELECT count(ser_sale_order_id), cfg_tbl_customer.txt_customer_code, cfg_tbl_customer.txt_customer_name\r\n" + 
				"     ,dealer.txt_customer_name as dealer\r\n" + 
				"   FROM sls_tbl_sale_order\r\n" + 
				"     LEFT JOIN cfg_tbl_customer ON cfg_tbl_customer.ser_customer_id = sls_tbl_sale_order.ser_customer_id\r\n" + 
				"     LEFT JOIN cfg_tbl_customer dealer ON dealer.ser_customer_id =  sls_tbl_sale_order.ser_dealer_id \r\n" + 
				"     where 1=1 and sls_tbl_sale_order.txt_status = 'APPROVED' and  sls_tbl_sale_order.ser_created_user_id > 0 ";
		String SQL2= "  group by cfg_tbl_customer.txt_customer_code,cfg_tbl_customer.txt_customer_name,dealer.txt_customer_name\r\n" + 
				"       order by dealer.txt_customer_name,cfg_tbl_customer.txt_customer_name ";
		
		
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getTxtUserName().equalsIgnoreCase("umair")) {
			
			SQL = String.valueOf(SQL) + " and ( sls_tbl_sale_order.ser_group_id =  "+ user.getSerGroupId() +"  or sls_tbl_sale_order.ser_product_id in (904,905,906,907,908,909)) " ;
			
//			query += " and ( serGroupId = " + user.getSerGroupId() + " or serProductId in (904,905,906,907,908,909)) ";

		}
		else if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			
			SQL = String.valueOf(SQL) + " and  sls_tbl_sale_order.ser_group_id =  "+ user.getSerGroupId() +" " ;
			
//			query += " and SaleOrder.serGroupId = " + user.getSerGroupId() + " ";

		}
		

		if (dto.getTxtDealer()!=null && dto.getTxtDealer().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and dealer.txt_customer_name  ilike '" + dto.getTxtDealer()
					+ "%'";
		
		
		if (dto.getTxtCustomer()!=null && dto.getTxtCustomer().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_customer.txt_customer_name  ilike '" + dto.getTxtCustomer()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  sls_tbl_sale_order.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  sls_tbl_sale_order.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		SQL = String.valueOf(SQL) +SQL2;
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	
////////////////////////////////////////////////////RPT Dispatch Report Customer Wise/////////////////

	@Override
	public List<Map<String, Object>> getBreakgeReportProcessWise(ReportDTO dto) {

		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

		String SQL = "   SELECT cfg_tbl_process.txt_process_name,cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,\r\n"
				+ "    cfg_tbl_product.txt_master_pack,cfg_tbl_product_quality.txt_product_quality_name,cfg_tbl_product_category.txt_product_category_name,\r\n"
				+ "    sum(pro_tbl_process_detail.num_process_qty) as num_process_qty,sum(pro_tbl_process_detail.num_approved_qty) as num_approved_qty,\r\n"
				+ "    sum(pro_tbl_process_detail.num_breakage) as num_breakage,sum(pro_tbl_process_detail.num_unit_wt * pro_tbl_process_detail.num_breakage) AS num_total_breakage_wt,\r\n"
				+ "    sum( pro_tbl_process_detail.num_unit_wt * pro_tbl_process_detail.num_approved_qty ) AS num_total_wt\r\n"
				+ " FROM pro_tbl_process_detail\r\n"
				+ "     LEFT JOIN cfg_tbl_product ON pro_tbl_process_detail.ser_product_id = cfg_tbl_product.ser_product_id\r\n"
				+ "     LEFT JOIN cfg_tbl_product_quality ON cfg_tbl_product_quality.ser_product_quality_id = pro_tbl_process_detail.ser_product_quality_id\r\n"
				+ "     LEFT JOIN cfg_tbl_product_design ON cfg_tbl_product_design.ser_product_design_id = pro_tbl_process_detail.ser_product_design_id\r\n"
				+ "     LEFT JOIN cfg_tbl_process ON cfg_tbl_process.ser_process_id = pro_tbl_process_detail.ser_process_id\r\n"
				+ "     LEFT JOIN cfg_tbl_product_category ON cfg_tbl_product_category.ser_product_category_id = cfg_tbl_product.ser_product_category_id  where 1=1 \r\n";
		String SQL2 = " group by cfg_tbl_process.txt_process_name,cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,\r\n"
				+ "    cfg_tbl_product.txt_master_pack,cfg_tbl_product_quality.txt_product_quality_name,cfg_tbl_product_category.txt_product_category_name ";

		if (dto.getSer_process_id() > 0)
			SQL = String.valueOf(SQL) + " and pro_tbl_process_detail.ser_process_id=" + dto.getSer_process_id();
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and pro_tbl_process_detail.ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and pro_tbl_process_detail.ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and pro_tbl_process_detail.ser_product_quality_id="
					+ dto.getSer_product_quality_id();

		if (dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and pro_tbl_process_detail.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and pro_tbl_process_detail.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		System.out.println("report sql :-" + SQL + SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL+ SQL2);
		return rs;
	}

	
	
	//////////////////////////////////////////////////// Process Detail Report
	//////////////////////////////////////////////////// /////////////////

	@Override
	public List<Map<String, Object>> getProcessDetailReportProcessWise(ReportDTO dto) {

		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

		String Date_From = "";
		String Date_TO = "";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				Date_From = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from()));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				Date_TO = DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to()));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		String SQL = "   Select\r\n"
				+ "cfg_tbl_product.ser_product_id,cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,cfg_tbl_product.txt_master_pack,cfg_tbl_product.txt_price_unit,cfg_tbl_product.num_units_in_master_pack,\r\n"
				+ "sum(inv_tbl_store_ledger_card.num_quantity_received)  as num_quantity_received ,sum(inv_tbl_store_ledger_card.num_quantity_issued) as num_quantity_issued,cfg_tbl_process.txt_process_name,cfg_tbl_process.ser_process_id,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_received) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  and  inv_tbl_store_ledger_card.ser_product_quality_id=1 and dte_date >= '"
				+ Date_From + "'  \r\n" + "and dte_date <='" + Date_TO
				+ "' ) ,0) as num_quantity_received_a,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_received) from inv_tbl_store_ledger_card where    inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  and inv_tbl_store_ledger_card.ser_product_quality_id=2 and dte_date >= '"
				+ Date_From + "'  \r\n" + "and dte_date <='" + Date_TO + "' ),0) as num_quantity_received_b,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_received) from inv_tbl_store_ledger_card where    inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  and inv_tbl_store_ledger_card.ser_product_quality_id=3 and dte_date >= '"
				+ Date_From + "'  \r\n" + "and dte_date <='" + Date_TO + "' ),0) as num_quantity_received_c,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_issued) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and ( inv_tbl_store_ledger_card.bln_is_breakage is null or inv_tbl_store_ledger_card.bln_is_breakage=false) and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  \r\n"
				+ "and  inv_tbl_store_ledger_card.ser_product_quality_id=1 and dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "' ),0)as num_quantity_issued_a,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_issued) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and ( inv_tbl_store_ledger_card.bln_is_breakage is null or inv_tbl_store_ledger_card.bln_is_breakage=false) and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  \r\n"
				+ "and  inv_tbl_store_ledger_card.ser_product_quality_id=2 and dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "' ),0)as num_quantity_issued_b,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_issued) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and ( inv_tbl_store_ledger_card.bln_is_breakage is null or inv_tbl_store_ledger_card.bln_is_breakage=false) and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  \r\n"
				+ "and  inv_tbl_store_ledger_card.ser_product_quality_id=3 and dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "' ),0)as num_quantity_issued_c,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_issued) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and inv_tbl_store_ledger_card.bln_is_breakage=true and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id \r\n"
				+ "and  inv_tbl_store_ledger_card.ser_product_quality_id=1 and dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "' ),0)as num_breakage_a,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_issued) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and inv_tbl_store_ledger_card.bln_is_breakage=true and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id  \r\n"
				+ "and  inv_tbl_store_ledger_card.ser_product_quality_id=2 and dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "' ),0)as num_breakage_b,\r\n"
				+ "COALESCE((Select sum(inv_tbl_store_ledger_card.num_quantity_issued) from inv_tbl_store_ledger_card where inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id  and inv_tbl_store_ledger_card.bln_is_breakage=true and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id \r\n"
				+ "and  inv_tbl_store_ledger_card.ser_product_quality_id=3 and dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "' ),0)as num_breakage_c,\r\n"
				+ "COALESCE((select num_balance_quantity from inv_tbl_store_ledger_card a where a.ser_store_ledger_card_id=(select max(ser_store_ledger_card_id)\r\n"
				+ " from inv_tbl_store_ledger_card where dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "'\r\n"
				+ "  and  inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id\r\n"
				+ "  and inv_tbl_store_ledger_card.ser_product_quality_id=1\r\n"
				+ "and inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id\r\n"
				+ "  )),0) as closing_balance_A,\r\n" + "\r\n"
				+ "  COALESCE((select num_balance_quantity from inv_tbl_store_ledger_card a where a.ser_store_ledger_card_id=(select max(ser_store_ledger_card_id)\r\n"
				+ " from inv_tbl_store_ledger_card where dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "'\r\n"
				+ "  and  inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id\r\n"
				+ "  and inv_tbl_store_ledger_card.ser_product_quality_id=2\r\n"
				+ "and inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id\r\n"
				+ "  )),0) as closing_balance_B,\r\n" + "\r\n"
				+ "  COALESCE((select num_balance_quantity from inv_tbl_store_ledger_card a where a.ser_store_ledger_card_id=(select max(ser_store_ledger_card_id)\r\n"
				+ " from inv_tbl_store_ledger_card where dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "'\r\n"
				+ "  and  inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id\r\n"
				+ "  and inv_tbl_store_ledger_card.ser_product_quality_id=3\r\n"
				+ "and inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id\r\n"
				+ "  )),0) as closing_balance_C,\r\n" + "\r\n" + "  \r\n"
				+ "   COALESCE((select num_opening_balance from inv_tbl_store_ledger_card where ser_store_ledger_card_id=(select min(ser_store_ledger_card_id)\r\n"
				+ " from inv_tbl_store_ledger_card where dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "'\r\n"
				+ " and  inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id\r\n"
				+ " and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id \r\n"
				+ " and inv_tbl_store_ledger_card.ser_product_quality_id=1  ) ),0)as opening_balance_A,\r\n" + "\r\n"
				+ "\r\n"
				+ "    COALESCE((select num_opening_balance from inv_tbl_store_ledger_card where ser_store_ledger_card_id=(select min(ser_store_ledger_card_id)\r\n"
				+ " from inv_tbl_store_ledger_card where dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "'\r\n"
				+ " and  inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id\r\n"
				+ " and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id \r\n"
				+ " and inv_tbl_store_ledger_card.ser_product_quality_id=2  ) ),0)as opening_balance_B,\r\n"
				+ "     COALESCE((select num_opening_balance from inv_tbl_store_ledger_card where ser_store_ledger_card_id=(select min(ser_store_ledger_card_id)\r\n"
				+ " from inv_tbl_store_ledger_card where dte_date >= '" + Date_From
				+ "'  and dte_date <='" + Date_TO + "'\r\n"
				+ " and  inv_tbl_store_ledger_card.ser_product_id=cfg_tbl_product.ser_product_id\r\n"
				+ " and  inv_tbl_store_ledger_card.ser_process_id=cfg_tbl_process.ser_process_id \r\n"
				+ " and inv_tbl_store_ledger_card.ser_product_quality_id=3  ) ),0)as opening_balance_C\r\n"
				+ "  ,COALESCE((select max(txt_layer_code) from pro_tbl_production_Detail  \n"
				+ " 		left outer join cfg_tbl_layer on cfg_tbl_layer.ser_layer_id=pro_tbl_production_Detail.ser_layer_id \n"
				+ " 		where dte_date >= '"+ Date_From+"'  and dte_date <='"+ Date_TO+"' and pro_tbl_production_Detail.ser_layer_id is not null \n"
				+ " 		 and  pro_tbl_production_Detail.ser_product_id=cfg_tbl_product.ser_product_id),'') as machine \n"
				
				+ " FROM inv_tbl_store_ledger_card\r\n"
				+ " LEFT JOIN cfg_tbl_product on inv_tbl_store_ledger_card.ser_product_id  = cfg_tbl_product.ser_product_id\r\n"
				+ " --LEFT JOIN cfg_tbl_product_quality ON cfg_tbl_product_quality.ser_product_quality_id = inv_tbl_store_ledger_card.ser_product_quality_id\r\n"
				+ " LEFT JOIN cfg_tbl_product_design ON cfg_tbl_product_design.ser_product_design_id = inv_tbl_store_ledger_card.ser_product_design_id\r\n"
				+ " LEFT JOIN cfg_tbl_process ON cfg_tbl_process.ser_process_id = inv_tbl_store_ledger_card.ser_process_id\r\n"
				+ " where 1=1 ";
		String SQL2 = " group by cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,cfg_tbl_product.txt_master_pack,\r\n"
				+ "    cfg_tbl_product.txt_price_unit, cfg_tbl_product.num_units_in_master_pack,cfg_tbl_product.ser_product_id\r\n"
				+ "    ,cfg_tbl_process.txt_process_name,cfg_tbl_process.ser_process_id ";

		if (dto.getSer_process_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_store_ledger_card.ser_process_id=" + dto.getSer_process_id();

		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_store_ledger_card.ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_store_ledger_card.ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_store_ledger_card.ser_product_quality_id="
					+ dto.getSer_product_quality_id();
		/*
		 * if (dto.getTxt_product_name().trim().length() > 0) SQL = String.valueOf(SQL)
		 * + " and cfg_tbl_product.txt_product_name  ilike '" +
		 * dto.getTxt_product_name() + "%'";
		 */

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and inv_tbl_store_ledger_card.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and inv_tbl_store_ledger_card.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String SQL_3= " order by cfg_tbl_product.txt_product_name ";
		System.out.println("report sql :-" + SQL + SQL2+ SQL_3);
		List rs = this.jdbcTemplateObject.queryForList(SQL + SQL2+ SQL_3);
	
		return rs;
	}
	
	
	
	
	@Override
	public List<Map<String, Object>> getDPRReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		
		System.out.println("------select * from view_dpr where 1=1  ");
		String SQL = "select * from view_dpr where 1=1  ";

		if (dto.getSer_process_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_process_id=" + dto.getSer_process_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id=" + dto.getSer_product_category_id();

		/*if (dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_product_name  ilike '" + dto.getTxt_product_name() + "%'";
*/		
		
		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and dte_createddate >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  dte_createddate <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

		SQL = String.valueOf(SQL) + " ";
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getStockDetailReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		
		System.out.println("------: select * from view_stock_detail where 1=1  ");
		String SQL = "select * from view_stock_detail where 1=1  ";

/*		if (dto.getSer_process_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_process_id=" + dto.getSer_process_id();*/

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id=" + dto.getSer_product_category_id();

		if (dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_product_name  ilike '" + dto.getTxt_product_name() + "%'";
		
		

		SQL = String.valueOf(SQL) + " ";
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	
	@Override
	public List<Map<String, Object>> getDispatchSummaryReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("view_dispatch_summary"));
		String SQL = rb.getString("view_dispatch_summary");

		if (dto.getSer_sale_order_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_sale_order_id=" + dto.getSer_sale_order_id();
		
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id="
					+ dto.getSer_product_quality_id();
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();

		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  issue_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  issue_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	

	@Override
	public List<Map<String, Object>> getWHReceivedReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

		String SQL = "   Select\r\n" + 
				"cfg_tbl_product.ser_product_id,cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,cfg_tbl_product.txt_master_pack,cfg_tbl_product.txt_price_unit,cfg_tbl_product.num_units_in_master_pack,\r\n" + 
				"sum(inv_tbl_store_ledger_card.num_quantity_received)  as num_quantity_received ,\r\n" + 
				"sum(inv_tbl_store_ledger_card.num_quantity_issued) as num_quantity_issued,\r\n" + 
				"cfg_tbl_process.txt_process_name,cfg_tbl_process.ser_process_id\r\n" + 
				"\r\n" + 
				" FROM inv_tbl_store_ledger_card\r\n" + 
				" LEFT JOIN cfg_tbl_product on inv_tbl_store_ledger_card.ser_product_id  = cfg_tbl_product.ser_product_id\r\n" + 
				" LEFT JOIN cfg_tbl_product_design ON cfg_tbl_product_design.ser_product_design_id = inv_tbl_store_ledger_card.ser_product_design_id\r\n" + 
				" LEFT JOIN cfg_tbl_process ON cfg_tbl_process.ser_process_id = inv_tbl_store_ledger_card.ser_process_id\r\n" + 
				" where 1=1  and inv_tbl_store_ledger_card.ser_process_id=5 ";
		String SQL2 = "  group by cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,cfg_tbl_product.txt_master_pack,\r\n" + 
				"    cfg_tbl_product.txt_price_unit, cfg_tbl_product.num_units_in_master_pack,cfg_tbl_product.ser_product_id\r\n" + 
				"    ,cfg_tbl_process.txt_process_name,cfg_tbl_process.ser_process_id ";
			
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

	/*	if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id="
					+ dto.getSer_product_quality_id();*/
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();

		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  inv_tbl_store_ledger_card.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  inv_tbl_store_ledger_card.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		

		System.out.println("report sql :-" + SQL +SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL+SQL2);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getWHIssuanceReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

		String SQL = "  SELECT cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,\r\n" + 
				"    cfg_tbl_product.txt_master_pack,cfg_tbl_product.txt_price_unit,cfg_tbl_product.num_units_in_master_pack,\r\n" + 
				"    cfg_tbl_product.num_pieces_in_master_pack,\r\n" + 
				"    cfg_tbl_product_category.txt_product_category_name,\r\n" + 
				"    sum(inv_tbl_issue_detail.num_quantity) as num_quantity,\r\n" + 
				"    sum(inv_tbl_issue_detail.num_qty_in_bags) as num_qty_in_bags,\r\n" + 
				"    sum(sls_tbl_so_detail.num_item_price*inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack) AS amount,\r\n" + 
				"    sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack) AS no_of_units,\r\n" + 
				"    sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight) AS toonage,\r\n" + 
				"    (sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight)/1000000) AS tons,\r\n" + 
				"   (ROUND(sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight)/sum(inv_tbl_issue_detail.num_quantity),2)) AS unit_wt,\r\n" + 
				"    sum(cfg_tbl_product.num_pieces_in_master_pack * inv_tbl_issue_detail.num_quantity) AS pcs\r\n" + 
				"    ,sum(sls_tbl_so_detail.num_item_price*inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack)/(sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight)/1000000) as rpt\r\n" + 
				",inv_tbl_issue_detail.ser_product_id\r\n" + 
				" FROM inv_tbl_issue_detail\r\n" + 
				"     LEFT JOIN inv_tbl_issue ON inv_tbl_issue_detail.ser_issue_id = inv_tbl_issue.ser_issue_id\r\n" + 
				"     LEFT JOIN sls_tbl_sale_order ON sls_tbl_sale_order.ser_sale_order_id = inv_tbl_issue.ser_sale_order_id\r\n" + 
				"     LEFT JOIN sls_tbl_so_detail ON inv_tbl_issue_detail.ser_so_detail_id = sls_tbl_so_detail.ser_so_detail_id\r\n" + 
				"     LEFT JOIN cfg_tbl_product ON cfg_tbl_product.ser_product_id = inv_tbl_issue_detail.ser_product_id\r\n" + 
				"     LEFT JOIN cfg_tbl_product_category ON cfg_tbl_product_category.ser_product_category_id = cfg_tbl_product.ser_product_category_id\r\n" + 
				"\r\n" + 
				"       LEFT JOIN cfg_tbl_brand ON cfg_tbl_product.ser_brand_id = cfg_tbl_brand.ser_brand_id\r\n" + 
				" where 1=1   ";
		String SQL2 = "  group by cfg_tbl_product.txt_product_code,cfg_tbl_product.txt_product_name,\r\n" + 
				"    cfg_tbl_product.txt_master_pack,cfg_tbl_product.txt_price_unit,cfg_tbl_product.num_units_in_master_pack,\r\n" + 
				"    cfg_tbl_product.num_pieces_in_master_pack,\r\n" + 
				"    cfg_tbl_product_category.txt_product_category_name,inv_tbl_issue_detail.ser_product_id\r\n" + 
				"    order by  cfg_tbl_product.txt_product_name ";
			
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

	/*	if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id="
					+ dto.getSer_product_quality_id();*/
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();

		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  inv_tbl_issue.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  inv_tbl_issue.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		

		System.out.println("report sql :-" + SQL+SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL+SQL2);
		return rs;
	}
	
	@Override
	public List<Map<String, Object>> getSaleInvoiceListReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		
		System.out.println("------: select * from view_sales_invoice_list where 1=1  ");
		String SQL = "select * from view_sales_invoice_list where 1=1  ";


		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		

		SQL = String.valueOf(SQL) + " ";
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getConsolidatedSalesSummaryReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

		String SQL = "  SELECT cfg_tbl_product.txt_product_name, cfg_tbl_product.txt_product_code, cfg_tbl_product_category.txt_product_category_name,COALESCE(sum(sls_tbl_so_detail.num_quantity),0) AS carton,\r\n" + 
				"COALESCE(sum(sls_tbl_so_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack),0) AS no_of_units,  COALESCE(sum(sls_tbl_so_detail.num_quantity * cfg_tbl_product.num_units_in_master_pack * sls_tbl_so_detail.num_item_price),0) AS sale_order_rate,  COALESCE(sum(inv_tbl_issue_detail.num_quantity),0) AS issue_carton, COALESCE(sum(cfg_tbl_product.num_units_in_master_pack * inv_tbl_issue_detail.num_quantity),0) AS issue_no_of_units,  COALESCE(sum(cfg_tbl_product.num_units_in_master_pack * inv_tbl_issue_detail.num_quantity * sls_tbl_so_detail.num_item_price),0) AS issue_rate,  COALESCE(sum(sls_tbl_invoice_detail.num_quantity),0) AS invoice_qty,  COALESCE(sum(cfg_tbl_product.num_units_in_master_pack * sls_tbl_invoice_detail.num_quantity),0) AS invoice_no_of_units,  COALESCE(sum(cfg_tbl_product.num_units_in_master_pack * sls_tbl_invoice_detail.num_quantity * sls_tbl_so_detail.num_item_price),0) AS invoice_rate , cfg_tbl_product_quality.txt_product_quality_name\r\n" + 
				"FROM sls_tbl_sale_order  LEFT JOIN sls_tbl_so_detail ON sls_tbl_sale_order.ser_sale_order_id = sls_tbl_so_detail.ser_sale_order_id\r\n" + 
				" LEFT JOIN inv_tbl_issue_detail ON inv_tbl_issue_detail.ser_so_detail_id = sls_tbl_so_detail.ser_so_detail_id  LEFT JOIN inv_tbl_issue ON inv_tbl_issue.ser_issue_id = inv_tbl_issue_detail.ser_issue_id  LEFT JOIN sls_tbl_invoice_detail ON inv_tbl_issue_detail.ser_issue_detail_id = sls_tbl_invoice_detail.ser_issue_detail_id\r\n" + 
				"\r\n" + 
				"LEFT JOIN sls_tbl_supplier_invoice ON sls_tbl_supplier_invoice.ser_supplier_invoice_id = sls_tbl_invoice_detail.ser_supplier_invoice_id  \r\n" + 
				"LEFT JOIN cfg_tbl_product ON cfg_tbl_product.ser_product_id = sls_tbl_so_detail.ser_product_id  \r\n" + 
				"LEFT JOIN cfg_tbl_product_category ON cfg_tbl_product.ser_product_category_id = cfg_tbl_product_category.ser_product_category_id\r\n" + 
				"LEFT JOIN cfg_tbl_product_quality ON sls_tbl_so_detail.ser_product_quality_id = cfg_tbl_product_quality.ser_product_quality_id\r\n" + 
				"    where 1 = 1   ";
		String SQL2 = "   GROUP BY cfg_tbl_product_category.txt_product_category_name, cfg_tbl_product.txt_product_name, cfg_tbl_product.txt_product_code , cfg_tbl_product_quality.ser_product_quality_id ORDER BY cfg_tbl_product_category.txt_product_category_name, cfg_tbl_product.txt_product_name ";
			
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

	/*	if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id="
					+ dto.getSer_product_quality_id();*/
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();

		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  inv_tbl_issue.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  inv_tbl_issue.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		

		System.out.println("report sql :-" + SQL+SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL+SQL2);
		return rs;
	}
	
	
	
	@Override
	public List<Map<String, Object>> getDCSummaryReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + "select * from view_delivery_challan_summary");
		String SQL ="select * from view_delivery_challan_summary where 1=1 ";
		
		if (dto.getSer_issue_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_issue_id=" + dto.getSer_issue_id();
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_product_quality_id="
					+ dto.getSer_product_quality_id();
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();
		
		
		if (dto.getTxt_mult_product()!=null &&  dto.getTxt_mult_product().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and ser_product_id in (" + dto.getTxt_mult_product()+")";

		if (dto.getTxt_mult_product_category()!=null &&  dto.getTxt_mult_product_category().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and ser_product_category_id in (" + dto.getTxt_mult_product_category()+")";

		if (dto.getTxt_mult_product_design()!=null &&  dto.getTxt_mult_product_design().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and ser_product_design_id in (" + dto.getTxt_mult_product_design()+")";
		
		if (dto.getTxt_mult_customer()!=null &&  dto.getTxt_mult_customer().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id in (" + dto.getTxt_mult_customer()+")";


		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		SQL = String.valueOf(SQL) + "  order by month_int  "; 
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		
		return rs;

}
	
	
	@Override
	public List<Map<String, Object>> getDCSummary_groupReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");

		String SQL = "  SELECT to_char(inv_tbl_issue.dte_date, 'MON-YY') as month,to_char(inv_tbl_issue.dte_date, 'YYMM') as month_int,\r\n" + 
				" sum(inv_tbl_issue_detail.num_quantity),\r\n" + 
				"     sum(inv_tbl_issue_detail.num_quantity * cfg_tbl_product.num_product_weight) AS toonage,\r\n" + 
				"    sum(cfg_tbl_product.num_pieces_in_master_pack * inv_tbl_issue_detail.num_quantity) AS pcs, cfg_tbl_customer.txt_customer_code,\r\n" + 
				"    cfg_tbl_customer.txt_customer_name,  cfg_tbl_city.txt_city_name, inv_tbl_issue.txt_station,\r\n" + 
				"    sum(cfg_tbl_product.num_pieces_in_master_pack * inv_tbl_issue_detail.num_quantity)/16000 as truck,\r\n" + 
				"    sum(inv_tbl_issue_detail.num_quantity*cfg_tbl_product.num_units_in_master_pack*sls_tbl_so_detail.num_item_price) as value\r\n" + 
				"   FROM inv_tbl_issue\r\n" + 
				"     LEFT JOIN inv_tbl_issue_detail ON inv_tbl_issue_detail.ser_issue_id = inv_tbl_issue.ser_issue_id\r\n" + 
				"     LEFT JOIN sls_tbl_sale_order ON sls_tbl_sale_order.ser_sale_order_id = inv_tbl_issue.ser_sale_order_id\r\n" + 
				"     LEFT JOIN sls_tbl_so_detail ON sls_tbl_so_detail.ser_so_detail_id = inv_tbl_issue_detail.ser_so_detail_id\r\n" + 
				"     LEFT JOIN cfg_tbl_product ON cfg_tbl_product.ser_product_id = inv_tbl_issue_detail.ser_product_id\r\n" + 
				"     LEFT JOIN cfg_tbl_product_category ON cfg_tbl_product_category.ser_product_category_id = cfg_tbl_product.ser_product_category_id\r\n" + 
				"     LEFT JOIN cfg_tbl_customer ON cfg_tbl_customer.ser_customer_id = sls_tbl_sale_order.ser_customer_id\r\n" + 
				"     LEFT JOIN cfg_tbl_brand ON cfg_tbl_product.ser_brand_id = cfg_tbl_brand.ser_brand_id\r\n" + 
				"     LEFT JOIN cfg_tbl_product_design ON inv_tbl_issue_detail.ser_product_design_id = cfg_tbl_product_design.ser_product_design_id\r\n" + 
				"     LEFT JOIN cfg_tbl_product_quality ON cfg_tbl_product_quality.ser_product_quality_id = inv_tbl_issue_detail.ser_product_quality_id\r\n" + 
				"     LEFT JOIN cfg_tbl_supplier ON cfg_tbl_supplier.ser_supplier_id = inv_tbl_issue.ser_supplier_id\r\n" + 
				"     LEFT JOIN cfg_tbl_city ON cfg_tbl_city.ser_city_id = cfg_tbl_customer.ser_city_id" + 
				"    where 1 = 1   ";
		String SQL2 = "   group by cfg_tbl_customer.txt_customer_code,\r\n" + 
				"    cfg_tbl_customer.txt_customer_name,  cfg_tbl_city.txt_city_name, inv_tbl_issue.txt_station,to_char(inv_tbl_issue.dte_date, 'MON-YY'),to_char(inv_tbl_issue.dte_date, 'YYMM')\r\n" + 
				"    order by to_char(inv_tbl_issue.dte_date, 'YYMM') ";
			
		
		if (dto.getSer_product_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_issue_detail.ser_product_id=" + dto.getSer_product_id();

		if (dto.getSer_product_design_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_issue_detail.ser_product_design_id="
					+ dto.getSer_product_design_id();

		if (dto.getSer_product_category_id() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.ser_product_category_id="
					+ dto.getSer_product_category_id();

		if (dto.getSer_product_quality_id() > 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_issue_detail.ser_product_quality_id="
					+ dto.getSer_product_quality_id();
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and sls_tbl_sale_order.ser_customer_id="
					+ dto.getSer_customer_id();
		
		if (dto.getTxt_mult_product()!=null &&  dto.getTxt_mult_product().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_issue_detail.ser_product_id in (" + dto.getTxt_mult_product()+")";

		if (dto.getTxt_mult_product_category()!=null &&  dto.getTxt_mult_product_category().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.ser_product_category_id in (" + dto.getTxt_mult_product_category()+")";

		if (dto.getTxt_mult_product_design()!=null &&  dto.getTxt_mult_product_design().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and inv_tbl_issue_detail.ser_product_design_id in (" + dto.getTxt_mult_product_design()+")";
		
		if (dto.getTxt_mult_customer()!=null &&  dto.getTxt_mult_customer().trim().length()> 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_customer.ser_customer_id in (" + dto.getTxt_mult_customer()+")";

		

		if (dto.getTxt_product_name()!=null && dto.getTxt_product_name().trim().length() > 0)
			SQL = String.valueOf(SQL) + " and cfg_tbl_product.txt_product_name  ilike '" + dto.getTxt_product_name()
					+ "%'";

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  inv_tbl_issue.dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  inv_tbl_issue.dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		

		System.out.println("report sql :-" + SQL+SQL2);
		List rs = this.jdbcTemplateObject.queryForList(SQL+SQL2);
		return rs;
	}
	
	
	
	@Override
	public List<Map<String, Object>> getGILPaymentReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + "select * from view_payments");
		String SQL ="select * from view_payments where 1=1 ";
		
		if (dto.getTxtStatus() !=null && dto.getTxtStatus().trim().length() >0 )
		{
			if(dto.getTxtStatus().equalsIgnoreCase("POSTED"))
			{
				SQL = String.valueOf(SQL) + " and (txt_xml_receive ilike '%<Success>Y</Success>%') " ;
			}
			else if(dto.getTxtStatus().equalsIgnoreCase("UNPOSTED"))
			{
				SQL = String.valueOf(SQL) + " and (txt_xml_receive ilike '%<Success>N</Success>%' or txt_xml_receive is null) " ;
			}
			else if(dto.getTxtStatus().equalsIgnoreCase("All"))
			{
//				SQL = String.valueOf(SQL) + " and (txt_status != 'Cancel' or txt_status is null) " ;
				
				SQL = String.valueOf(SQL) + " and (txt_status ilike ( 'InProgress') or txt_status ilike ( 'Approved') or txt_status is null) " ;
			}
			
			else if(dto.getTxtStatus().equalsIgnoreCase("Cancel"))
			{
				SQL = String.valueOf(SQL) + " and (txt_status ilike 'Cancel' ) " ;
				
				
			}
			
		}
		
		if (dto.getSer_customer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_customer_id="
					+ dto.getSer_customer_id();
		
		
		if (dto.getSer_dealer_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_dealer_id="
					+ dto.getSer_dealer_id();
		
		
		if(dto.getTxt_so_no()!=null && dto.getTxt_so_no().trim().length() > 0)
		{
			SQL = String.valueOf(SQL) + " and (PSONO ilike '%"+dto.getTxt_so_no()+"%') " ;
		}
		
		
		CfgTblUser user = this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

		if (user != null && user.getTxtUserName().equalsIgnoreCase("umair")) {
			
			SQL = String.valueOf(SQL) + " and ( serGroupId =  "+ user.getSerGroupId() +"  or serProductId in (904,905,906,907,908,909)) " ;
			
//			query += " and ( serGroupId = " + user.getSerGroupId() + " or serProductId in (904,905,906,907,908,909)) ";

		}
		else if (user != null && user.getSerGroupId() != null && user.getSerGroupId() > 0) {
			
			SQL = String.valueOf(SQL) + " and  serGroupId =  "+ user.getSerGroupId() +" " ;
			
//			query += " and SaleOrder.serGroupId = " + user.getSerGroupId() + " ";

		}

		if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
			try {

				SQL = String.valueOf(SQL) + " and  dte_date >= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())) + "'";
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
			try {
				SQL = String.valueOf(SQL) + " and  dte_date <= '"
						+ DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())) + "'";
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		SQL = String.valueOf(SQL) + "  order by ser_sale_order_id  "; 
		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getJOBCardReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
//		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
//		System.out.println("------" + rb.getString("View_job_card"));
		String SQL = "select * from View_job_card where 1=1 ";

		if (dto.getSer_wo_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_work_order_id=" + dto.getSer_wo_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getTIRReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
//		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
//		System.out.println("------" + rb.getString("View_TIR"));
		String SQL = "select * from View_tir  where 1=1";

		if (dto.getSer_tir_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_tir_id=" + dto.getSer_tir_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getInvoiceReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
		System.out.println("------" + rb.getString("view_sales_tax_invoice"));
		String SQL = rb.getString("view_sales_tax_invoice");

		if (dto.getSer_invoice_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_supplier_invoice_id=" + dto.getSer_invoice_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	
	@Override
	public List<Map<String, Object>> getToolReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
//		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
//		System.out.println("------" + rb.getString("View_TIR"));
		String SQL = "select * from view_checklist  where 1=1";

		if (dto.getSer_sale_order_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_sale_order_id=" + dto.getSer_sale_order_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
	@Override
	public List<Map<String, Object>> getClaimReport(ReportDTO dto) {
		this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
//		ResourceBundle rb = ResourceBundle.getBundle("properties.Query");
//		System.out.println("------" + rb.getString("View_TIR"));
		String SQL = "select * from view_free_service  where 1=1";

		if (dto.getSer_claim_id() > 0)
			SQL = String.valueOf(SQL) + " and ser_claim_id=" + dto.getSer_claim_id();

		System.out.println("report sql :-" + SQL);
		List rs = this.jdbcTemplateObject.queryForList(SQL);
		return rs;
	}
	
}

