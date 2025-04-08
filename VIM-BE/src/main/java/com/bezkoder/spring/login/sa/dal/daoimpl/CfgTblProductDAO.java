package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

import javax.persistence.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.bll.dto.SPDTO;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductDAO;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;

@Repository
public class CfgTblProductDAO implements ICfgTblProductDAO {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Autowired
	private ICommonService commonService;

	 @Autowired
	  private LoginDAO loginDao;
	 
	private static final Logger log = LoggerFactory.getLogger(CfgTblProductDAO.class);

	public CfgTblProductDAO() {
		// TODO Auto-generated constructor stub
	}

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllProduct() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
		List<CfgTblProduct> Products=new ArrayList();
	//	CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//{
//	Products = entityManager
//				.createQuery("FROM CfgTblProduct where  (blIspacking=FALSE or blIspacking is null) and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//}
//else
	Products = entityManager.createQuery("FROM CfgTblProduct where blIsDeleted=FALSE  and (blIspacking=FALSE or blIspacking is null)")
	.getResultList();

		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blIsDeleted=FALSE and (blIspacking=FALSE or blIspacking is null)")
				.getResultList();*/

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllPacking() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where   blIspacking=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where  blIsDeleted=FALSE and blIspacking=TRUE)")
	.getResultList();

		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where  blIsDeleted=FALSE and blIspacking=TRUE")
				.getResultList();*/

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllInventory() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsInventoryItem=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blnIsInventoryItem=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsInventoryItem=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllSpareParts() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsInventoryItem=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blnIsInventoryItem=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("select new com.bezkoder.spring.login.admin.bll.dto.SPDTO(pp.serProductId,pp.txtProductCode,pp.txtProductName,pp.txtQuality,pp.blnStatus,pp.numSalePrice,pp.txtType) FROM CfgTblProduct pp where blnStatus=TRUE and blIsDeleted=FALSE and blnIsInventoryItem=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllSalesItem() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsSaleItem=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		and  blnIsSaleItem=TRUE
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE  and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsSaleItem=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllPurchaseItem() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsPurchaseItem=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blnIsPurchaseItem=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blnIsPurchaseItem=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllImportItem() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsImport=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blIsImport=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsImport=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllSetItem() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsSet=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blIsSet=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsSet=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllComponentItem() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsComponent=TRUE ")
				.getResultList();*/
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blIsComponent=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsComponent=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllProductionItem() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsProduction=TRUE ")
				.getResultList();*/
		
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blIsProduction=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsProduction=TRUE)")
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getAllProductionItem(String priceGroup) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsProduction=TRUE ")
				.getResultList();*/
		
		
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
		String whereClause ="";
		if(priceGroup !=null && priceGroup.indexOf("PickUp") >= 0)
			whereClause =whereClause +" and blIsPickup = TRUE " ;
		
		else if(priceGroup !=null && priceGroup.indexOf("Truck") >= 0)
			whereClause =whereClause +" and blIsTruck = TRUE " ;
		
		else if(priceGroup !=null && priceGroup.indexOf("Bus") >= 0)
			whereClause =whereClause +" and blIsBus = TRUE " ;
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and  blIsProduction=TRUE and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and blIsProduction=TRUE) "+whereClause)
	.getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getActiveProduct() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		/*List<CfgTblProduct> Products = entityManager
				.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and (blIspacking=FALSE or blIspacking is null)").getResultList();
*/
		List<CfgTblProduct> Products=new ArrayList();
		CfgTblUser user=this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());
		
//		if(user !=null 	&& user.getSerGroupId()!=null && user.getSerGroupId() >0)
//		{
//			Products = entityManager
//						.createQuery("FROM CfgTblProduct where blnStatus=TRUE and   (blIspacking=FALSE or blIspacking is null) and serGroupId= "+user.getSerGroupId()+" and blIsDeleted=FALSE").getResultList();
//		}
//		else
			Products = entityManager.createQuery("FROM CfgTblProduct where blnStatus=TRUE and blIsDeleted=FALSE and  (blIspacking=FALSE or blIspacking is null))")
	.getResultList();
		
		entityManager.getTransaction().commit();
		entityManager.close();

		return Products;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CfgTblProduct> getProductByProperty(String property, String value, String mode, String oldValue) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProduct where " + property + "='" + value + "'";
			if (mode.equals("Edit")) {
				query += " and serBranchId <> " + oldValue;
			}
			List<CfgTblProduct> Products = entityManager.createQuery(query).getResultList();

			entityManager.close();
			return Products;
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
	public String addNewProduct(CfgTblProduct CfgTblProduct) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			CfgTblProduct.setBlnStatus(true);
			CfgTblProduct.setBlIsDeleted(false);
			CfgTblProduct.setSerCreatedUserId(commonService.getCurrentLoggedInUser());
			CfgTblProduct.setDteCreateddate(commonService.getCurrentTimeStamp_new());
			
			  
		//	CfgTblProduct.setSerGroupId(this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser()).getSerGroupId());
			entityManager.persist(CfgTblProduct);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String deleteProduct(List<String> ProductsId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			for (String serProductId : ProductsId) {
				CfgTblProduct Product = entityManager.find(CfgTblProduct.class, Integer.parseInt(serProductId));
				if (Product != null) {
					Product.setBlIsDeleted(true);

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
	public String updateProduct(CfgTblProduct CfgTblProduct) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(CfgTblProduct);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return "Failure";
		}
	}

	@Override
	public String generateProductNo(String type) {
		// int ProductNo;
		String ProductType = type;
		// String ProductCode="";
		int ord_no = 0;
		int ord_no1 = 0;
		EntityManager entityManager = getEntityManager();
		if (ProductType.equalsIgnoreCase("Packing")) {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductCode) from CfgTblProduct where blIspacking=TRUE")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "Prod-00";
				}
				ord_no = Integer.valueOf(zoneCode.substring(5));

				ord_no = ord_no + 1;
				String code = "Prod-1";
				if (ord_no < 10)
					code = "Prod-00" + ord_no;
				else if (ord_no > 9 && ord_no < 100)
					code = "Prod-0" + ord_no;
				else
					code = "Prod-" + ord_no;
				entityManager.close();
				return code;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		} else {
			try {
				entityManager.getTransaction().begin();

				String zoneCode = (String) entityManager.createQuery("select MAX(txtProductCode) from CfgTblProduct where ( blIspacking=FALSE or blIspacking is null ) ")
						.getSingleResult();
				if (isNullOrEmpty(zoneCode)) {

					zoneCode = "Prod-00";
				}
				ord_no1 = Integer.valueOf(zoneCode.substring(5));

				ord_no1 = ord_no1 + 1;
				String code = "Prod-1";
				if (ord_no1 < 10)
					code = "Prod-00" + ord_no1;
				else if (ord_no1 > 9 && ord_no1 < 100)
					code = "Prod-0" + ord_no1;
				else
					code = "Prod-" + ord_no1;
				entityManager.close();
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

	public String getProductById(String ProductId) {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			String query = "FROM CfgTblProduct where txtProductCode='" + ProductId + "'";

			List<CfgTblProduct> Product = entityManager.createQuery(query).getResultList();

			entityManager.close();
			if (Product.size() > 0) {
				return String.valueOf(Product.size());
			}
			return "0";

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
	
	@Override
	public List<CfgTblProduct> searchProduct(CfgTblProduct Product) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "from CfgTblProduct Product where 1=1 ";
	    if(Product.getTxtProductCode() != null)
	    {
	    	query+=" and upper(Product.txtProductCode) like"+" upper('"+Product.getTxtProductCode()+"%')"+" ";
	    }
	    if(Product.getTxtProductName() !=null){
	    	query+=" and upper(Product.txtProductName) like"+" upper('"+Product.getTxtProductName()+"%')"+"  ";
	    }
	    
	  
	    
	    /*if(Product.getTxtEmail() !=null){
	    	query+=" and upper(Product.txtEmail) like"+" upper('"+Product.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Product.getSerProductId() !=null){
	    	query+=" and Product.serProductId ="+" "+Product.getSerProductId()+""+"  ";
	    }
	  
	    query+=" order by Product.serProductId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
	    List<CfgTblProduct> cust = entityManager.createQuery(
	    		subQuery).getResultList();
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
	
	
	@Override
	public List<SPDTO> searchProductSP(CfgTblProduct Product) {
			EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = "select new com.bezkoder.spring.login.admin.bll.dto.SPDTO(Product.serProductId,Product.txtProductCode,Product.txtProductName,Product.txtQuality,Product.blnStatus,Product.numSalePrice) from CfgTblProduct Product where 1=1 ";
	    if(Product.getTxtProductCode() != null)
	    {
	    	query+=" and upper(Product.txtProductCode) like"+" upper('"+Product.getTxtProductCode()+"%')"+" ";
	    }
	    if(Product.getTxtProductName() !=null){
	    	query+=" and upper(Product.txtProductName) like"+" upper('"+Product.getTxtProductName()+"%')"+"  ";
	    }
	    
	    
	    if(Product.getTxtQuality() !=null){
	    	query+=" and upper(Product.txtQuality) like"+" upper('"+Product.getTxtQuality()+"')"+"  ";
	    }
	  
	    
	    /*if(Product.getTxtEmail() !=null){
	    	query+=" and upper(Product.txtEmail) like"+" upper('"+Product.getTxtEmail()+"')"+"  ";
	    }*/
	    
	    if(Product.getSerProductId() !=null){
	    	query+=" and Product.serProductId ="+" "+Product.getSerProductId()+""+"  ";
	    }
	  
	    query+="  and Product.blnStatus=TRUE and (Product.blIsDeleted=FALSE or blIsDeleted is null) and Product.blnIsInventoryItem=TRUE order by Product.serProductId  DESC";
	    log.info("Query is ---"+query.substring(0, query.length()));
	    
	    System.out.println("query ----:"+query.substring(0, query.length()));
	    String subQuery = query.substring(0, query.length());
//	    List<CfgTblProduct> cust = entityManager.createQuery(
//	    		subQuery).getResultList();
	    
	    
  TypedQuery<SPDTO> queryOut= (TypedQuery<SPDTO>) entityManager.createQuery(subQuery);
	    
	    queryOut.setFirstResult(Product.getNumOldPrice()!=null ? Product.getNumOldPrice().intValue()
	    		:0);
	    queryOut.setMaxResults(500);
	    List<SPDTO> cust = queryOut.getResultList();
	    
	    
	    entityManager.getTransaction().commit();
	    entityManager.close();
	 
	    return cust;
	}
}
