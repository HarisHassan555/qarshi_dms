package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.SimpleDateFormat;

import javax.persistence.*;;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductComponentDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProductComponent;

@Repository
public class CfgTblProductComponentDAO implements ICfgTblProductComponentDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	private static final Logger log = LoggerFactory.getLogger(CfgTblProductComponentDAO.class);

	public CfgTblProductComponentDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductComponent> getAllProductComponent() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblProductComponent> ProductComponents = entityManager.createQuery("FROM CfgTblProductComponent where blIsDeleted=FALSE")
				.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductComponents;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductComponent> getActiveProductComponent() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		List<CfgTblProductComponent> ProductComponents = entityManager
				.createQuery("FROM CfgTblProductComponent where blnStatus=TRUE and blIsDeleted=FALSE").getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return ProductComponents;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProductComponent> getProductComponentByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductComponent where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblProductComponent> ProductComponents = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return ProductComponents;
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
	public String addNewProductComponentinList(List<CfgTblProductComponent> lstcfgTblProductComponents) {
		
		CfgTblProductComponent cfgTblProductComponent_sep;
//		List<CfgTblProductComponent> lst_for_updation=new ArrayList();
		List<CfgTblProductComponent> lst_for_new=new ArrayList();
		 Iterator<CfgTblProductComponent> itr_sep = lstcfgTblProductComponents.iterator();
	      while (itr_sep.hasNext())
	      {
	    	  cfgTblProductComponent_sep = (CfgTblProductComponent) itr_sep.next();
	    	  if(cfgTblProductComponent_sep.getSerProductComponentId()!=null)
	    	  {
//	    		  lst_for_updation.add(cfgTblProductComponent_sep);
	    		  updateProductComponent(cfgTblProductComponent_sep);
	    	  }
	    	  else
	    	  {
	    		  lst_for_new.add(cfgTblProductComponent_sep);
	    	  }
	      }
		
		if(lst_for_new!=null && lst_for_new.size() >0 )
		{
			EntityManager entityManager = getEntityManager();
			try {
				entityManager.getTransaction().begin();
				CfgTblProductComponent cfgTblProductComponent;
				 Iterator<CfgTblProductComponent> itr = lst_for_new.iterator();
			      while (itr.hasNext())
			      {	
					cfgTblProductComponent = (CfgTblProductComponent) itr.next();
					cfgTblProductComponent.setBlnStatus(true);
					cfgTblProductComponent.setBlIsDeleted(false);
					cfgTblProductComponent.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
					cfgTblProductComponent.setDteCreateddate(commonService.getCurrentTimeStamp_new());
					entityManager.persist(cfgTblProductComponent);
					// addNewProductComponent(cfgTblProductComponent);
			      }
				
				entityManager.getTransaction().commit();
				entityManager.close();
				return "Success";
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				return "Failure";
			}
		/*	CfgTblProductComponent cfgTblProductComponent;
			 Iterator<CfgTblProductComponent> itr = lstcfgTblProductComponents.iterator();
		      while (itr.hasNext())
		      {
		    	  cfgTblProductComponent = (CfgTblProductComponent)itr.next();
		    	  addNewProductComponent(cfgTblProductComponent);
		      }*/
		      
		      
		
			
		}
		else
			return "Success";
		/*EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblProductComponent.setBlnStatus(true);
			CfgTblProductComponent.setBlIsDeleted(false);
			entityManager.persist(CfgTblProductComponent);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}*/
	}
	
	
	@Override
	public String addNewProductComponent(CfgTblProductComponent cfgTblProductComponent) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			cfgTblProductComponent.setBlnStatus(true);
			cfgTblProductComponent.setBlIsDeleted(false);
			cfgTblProductComponent.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			cfgTblProductComponent.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			entityManager.persist(cfgTblProductComponent);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteProductComponent(List<String> ProductComponentsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProductComponentId : ProductComponentsId) {
				CfgTblProductComponent ProductComponent = entityManager.find(CfgTblProductComponent.class, Integer.parseInt(serProductComponentId));
				if (ProductComponent != null) {
					ProductComponent.setBlIsDeleted(true);

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
	public String updateProductComponent(CfgTblProductComponent CfgTblProductComponent) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblProductComponent);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProductComponentNo(String type) {
		// int ProductComponentNo;
		String ProductComponentType = type;
		// String ProductComponentCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ProductComponentType.equals("1")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductComponentCode) from CfgTblProductComponent ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "PCAT-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(3));

				ord_no = ord_no + 1;
				String code = "PCAT-1";
				if (ord_no < 10)
					code = "PCAT-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "PCAT-0" + ord_no;
				else
					code = "PCAT-" + ord_no;
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductComponentCode) from CfgTblProductComponent ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "PCAT-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(5));

				ord_no1 = ord_no1 + 1;
				String code = "PCAT-1";
				if (ord_no1 < 10)
					code = "PCAT-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "PCAT-0" + ord_no1;
				else
					code = "PCAT-" + ord_no1;
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

	public String getProductComponentById(String ProductComponentId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProductComponent where txtProductComponentCode='" + ProductComponentId + "'";

			List<CfgTblProductComponent> ProductComponent = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (ProductComponent.size() > 0) {
				return String.valueOf(ProductComponent.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblProductComponent> searchProductComponent(CfgTblProductComponent ProductComponent) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblProductComponent ProductComponent where 1=1 and  (blIsDeleted=FALSE or blIsDeleted is null ) ";
	    if(ProductComponent.getCfgTblProductParent()!= null  && ProductComponent.getCfgTblProductParent().getSerProductId()!= null)
	    {
	    	query+=" and ProductComponent.cfgTblProductParent.serProductId ="+ProductComponent.getCfgTblProductParent().getSerProductId();
	    }
	  /*  if(ProductComponent.getTxtProductComponentName() !=null){
	    	query+=" and upper(ProductComponent.txtProductComponentName) like"+" upper('"+ProductComponent.getTxtProductComponentName()+"%')"+"  ";
	    }*/
	    
//	    if(vehiclelogs!=null && vehiclelogs.size()>0)
//			 pre =(String)entityManager.createQuery("select txtOdoMeterReading from CitTableVehicleLog log  where serVehicleLogId =(select max(ff.serVehicleLogId) from CitTableVehicleLog ff where ff.citTableVehicle.serVehicleId ='"+type1+"')").getSingleResult();
//			else
//				pre=entityManager.createQuery("select numOdoMeterReading from CitTableVehicle where serVehicleId='"+type1+"'").getSingleResult().toString();
//			
	    
	    /*if(ProductComponent.getTxtEmail() !=null){
	    	query+=" and upper(ProductComponent.txtEmail) like"+" upper('"+ProductComponent.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(ProductComponent.getSerProductComponentId() !=null){
	    	query+=" and ProductComponent.serProductComponentId ="+" "+ProductComponent.getSerProductComponentId()+""+"  ";
	    }
	    
	    if(ProductComponent.getCfgTblCustomer()!= null  && ProductComponent.getCfgTblCustomer().getSerCustomerId()!= null)
	    {
	    	
	    	if( ProductComponent.getCfgTblCustomer().getBlIsGroup() !=null && ProductComponent.getCfgTblCustomer().getBlIsGroup() )
		    	{
	    		    if(ProductComponent.getCfgTblCustomer().getCfgTblGroupCustomer()!=null && ProductComponent.getCfgTblCustomer().getCfgTblGroupCustomer().getSerCustomerId()!=null )
	    		    {
				    	if(ProductComponent.getCfgTblCustomer().getBlIsLabsa()!=null && ProductComponent.getCfgTblCustomer().getBlIsLabsa())
				    	{
				    		query+=" and ProductComponent.cfgTblCustomer.serCustomerId in ( select cust.serCustomerId  FROM CfgTblCustomer cust where "
				    				+ "(cust.serCustomerId="+ProductComponent.getCfgTblCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +ProductComponent.getCfgTblCustomer().getSerCustomerId()+" "
				    						+ " or cust.serCustomerId="+ProductComponent.getCfgTblCustomer().getCfgTblGroupCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +ProductComponent.getCfgTblCustomer().getCfgTblGroupCustomer().getSerCustomerId() +") "
				    						+ " and  cust.blIsLabsa = True ) ";
				    	   				
				    	}
				    	else
				    	{
				    		
					    		query+=" and ProductComponent.cfgTblCustomer.serCustomerId in ( select cust.serCustomerId  FROM CfgTblCustomer cust where "
					    				+ "(cust.serCustomerId="+ProductComponent.getCfgTblCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +ProductComponent.getCfgTblCustomer().getSerCustomerId()+"  "
					    						+ " or cust.serCustomerId="+ProductComponent.getCfgTblCustomer().getCfgTblGroupCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +ProductComponent.getCfgTblCustomer().getCfgTblGroupCustomer().getSerCustomerId() +") "
					    						+ " and  ( cust.blIsLabsa = false  or cust.blIsLabsa is null )  ) ";
					    	   				
					    	
				    	}
	    		    }
	    		    else
	    		    {
	    		    	if(ProductComponent.getCfgTblCustomer().getBlIsLabsa()!=null && ProductComponent.getCfgTblCustomer().getBlIsLabsa())
				    	{
				    		query+=" and ProductComponent.cfgTblCustomer.serCustomerId in ( select cust.serCustomerId  FROM CfgTblCustomer cust where "
				    				+ "(cust.serCustomerId="+ProductComponent.getCfgTblCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +ProductComponent.getCfgTblCustomer().getSerCustomerId()+" ) "
				    							+ " and  cust.blIsLabsa = True ) ";
				    	   				
				    	}
				    	else
				    	{
				    		
					    		query+=" and ProductComponent.cfgTblCustomer.serCustomerId in ( select cust.serCustomerId  FROM CfgTblCustomer cust where "
					    				+ "(cust.serCustomerId="+ProductComponent.getCfgTblCustomer().getSerCustomerId()+" or cust.cfgTblGroupCustomer.serCustomerId  =" +ProductComponent.getCfgTblCustomer().getSerCustomerId()+" ) "
					    					+ " and  ( cust.blIsLabsa = false  or cust.blIsLabsa is null )  ) ";
					    	   				
					    	
				    	}
	    		    }
		    	}
	    	else
	    		  	query+=" and ProductComponent.cfgTblCustomer.serCustomerId ="+ProductComponent.getCfgTblCustomer().getSerCustomerId();
	    }
	  
	    query+=" order by ProductComponent.serProductComponentId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblProductComponent> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
