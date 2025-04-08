package com.bezkoder.spring.login.sa.dal.daoimpl;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bezkoder.spring.login.admin.bll.dto.DashBoardDto;
import com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto;

import javax.persistence.*;;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.dal.dao.IDashBoardDAO;

import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;


@Repository
public class DashBoardDAO implements IDashBoardDAO{

	@Autowired public ICommonService commonService;
	@Autowired public EntityManagerFactory entityManagerFactory;
	public static final Logger log = LoggerFactory.getLogger(DashBoardDAO.class);

	public DashBoardDAO() {
 	}

	public EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}
 
	// 50
	public List<DashBoardDto> getTopMostUsedServices(Integer howManyRecordRequired) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardDto (product.serProductId, product.txtProductName,  sum(detaill.numQuantity) ) "
	         		 + " FROM  SlsTblWorkOrderDetail detaill "
	    		     + " LEFT JOIN detaill.cfgTblProduct product "
	    		     + " WHERE product.blIspacking is true AND detaill.blIsService  is true "
	    		     + " GROUP BY product.serProductId, product.txtProductName order by sum(detaill.numQuantity) DESC";
 
	    List<DashBoardDto> list = entityManager.createQuery(query).setMaxResults(howManyRecordRequired).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	//49
	public List<DashBoardDto> getTopMostUsedSpareParts(Integer howManyRecordRequired) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardDto (product.serProductId, product.txtProductName,  sum(detaill.numQuantity) ) "
	         		 + " FROM  SlsTblWorkOrderDetail detaill "
	    		     + " LEFT JOIN detaill.cfgTblProduct product "
	    		     + " WHERE product.blnIsInventoryItem is true AND detaill.blIsSP  is true "
	    		     + " GROUP BY product.serProductId, product.txtProductName order by sum(detaill.numQuantity) DESC";
 
	    List<DashBoardDto> list = entityManager.createQuery(query).setMaxResults(howManyRecordRequired).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	//47
	public List<DashBoardRevenueDto> getRevenueGenerationFromPartsMonthNDealerWise(Date dateFrom, Date dateTo) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto ( "
	    		     + " CAST( CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) )  as java.lang.String) ,"
	    		     + " CAST( dealer.txtCustomerCode as java.lang.String)  ,"
	    		     + " CAST( dealer.txtCustomerName as java.lang.String)  ,"
	    		     + " CAST( SUM( COALESCE(detaill.numPricePerPiece , 0) * COALESCE( detaill.numQuantity, 0) ) as java.math.BigDecimal )  ,  "
	    		     + " CAST( dealer.serCustomerId as java.lang.Integer )  "
	    		     + " ) "
	         		 + " FROM SlsTblWorkOrder as base "
	         		 + " LEFT JOIN base.cfgTblDealer as dealer "
	         		 + " LEFT JOIN base.slsTblWorkOrderDetail as detaill "
	    		     + " LEFT JOIN detaill.cfgTblProduct as product "
	    		     + " WHERE detaill.blIsSP  is true "
	    		  //   + " AND base.dteDate between :dateFrom AND :dateTo "
	    		     + " GROUP BY CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) ) , dealer.serCustomerId "
	    		     + " ORDER BY CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) ) , dealer.serCustomerId ";
 
//	    List<DashBoardRevenueDto> list = entityManager.createQuery(query).setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo).getResultList() ;
	    List<DashBoardRevenueDto> list = entityManager.createQuery(query).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	// 45
	public List<DashBoardRevenueDto> getRevenueGenerationFromServiceMonthlyNDealerWise(Date dateFrom, Date dateTo) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto ( "
	    		     + " CAST( CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) )  as java.lang.String) ,"
	    		     + " CAST( dealer.txtCustomerCode as java.lang.String)  ,"
	    		     + " CAST( dealer.txtCustomerName as java.lang.String)  ,"
	    		     + " CAST( SUM( COALESCE(detaill.numPricePerPiece , 0) * COALESCE( detaill.numQuantity, 0) ) as java.math.BigDecimal )  ,  "
	    		     + " CAST( dealer.serCustomerId as java.lang.Integer )  "
	    		     + " ) "
	         		 + " FROM SlsTblWorkOrder as base "
	         		 + " LEFT JOIN base.cfgTblDealer as dealer "
	         		 + " LEFT JOIN base.slsTblWorkOrderDetail as detaill "
	    		     + " LEFT JOIN detaill.cfgTblProduct as product "
	    		     + " WHERE  detaill.blIsService  is true "
	    		//     + " AND base.dteDate between :dateFrom AND :dateTo "
	    		     + " GROUP BY CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) ) , dealer.serCustomerId "
	    		     + " ORDER BY CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) ) , dealer.serCustomerId ";
 
//	    List<DashBoardRevenueDto> list = entityManager.createQuery(query).setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo).getResultList() ;
	    List<DashBoardRevenueDto> list = entityManager.createQuery(query).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	//48
	public List<DashBoardDto> getTopDefectWisePhenomenCount(Integer howManyRecordRequired) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardDto (phenomenon.serPhenomenonId, phenomenon.txtPhenomenonName,  COUNT(phenomenon.serPhenomenonId) ) "
	         		 + " FROM  SlsTblTIR base "
	    		     + " LEFT JOIN base.cfgTblPhenomenon phenomenon "
 	    		     + " GROUP BY phenomenon.serPhenomenonId, phenomenon.txtPhenomenonName "
 	    		     + " order by COUNT(phenomenon.serPhenomenonId) DESC";
 
	    List<DashBoardDto> list = entityManager.createQuery(query).setMaxResults(howManyRecordRequired).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	// 46 not mature just sum of work order detail
	// 
	public List<DashBoardRevenueDto> getWarrantyCostMonthWise(Date dateFrom, Date dateTo) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto ( "
	    		     + " CAST( CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) )  as java.lang.String ) ,"
	    		     + " CAST( dealer.txtCustomerName as java.lang.String)  ,"
	    		     + " CAST( dealer.txtCustomerCode as java.lang.String)  ,"
 	    		     + " CAST( SUM( COALESCE(detaill.numPricePerPiece , 0) * COALESCE( detaill.numQuantity, 0) ) as java.math.BigDecimal )  "
 	    		     + " ) "
	         		 + " FROM SlsTblWorkOrder as base "
 	         		 + " LEFT JOIN base.slsTblWorkOrderDetail as detaill "
 	         		 + " LEFT JOIN base.cfgTblDealer as dealer "
 	    		//     + " WHERE base.dteDate between :dateFrom AND :dateTo "
	    		     + " GROUP BY CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) ),dealer.txtCustomerCode,dealer.txtCustomerName"
	    		     + " ORDER BY CONCAT( Month(base.dteDate) , '-' , YEAR(base.dteDate) ),dealer.txtCustomerCode " ;
 
//	    List<DashBoardDto> list = entityManager.createQuery(query).setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo).getResultList() ;
	    List<DashBoardRevenueDto> list = entityManager.createQuery(query).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	
	
	public List<DashBoardRevenueDto> getCustomerRetension(Date dateFrom, Date dateTo) {
		EntityManager entityManager = getEntityManager();
	    entityManager.getTransaction().begin();
	    String query = " SELECT new com.bezkoder.spring.login.admin.bll.dto.DashBoardRevenueDto ( "
	    		    
	    		     + " CAST( dealer.txtCustomerName as java.lang.String)  ,"
	    		     + " CAST( dealer.txtCustomerCode as java.lang.String)  ,"
	    		     + " CAST( dealer.txtDisplayAddress as java.lang.String)  ,"
	    		     + " CAST( dealer.txtNtnNo as java.lang.String)  ,"
	    		     + " CAST( dealer.txtCnicNo as java.lang.String)  "

 	    		     + " ) "
	         		 + " FROM SlsTblWorkOrder as base "
 	         		 + " LEFT JOIN base.slsTblWorkOrderDetail as detaill "
 	         		 + " LEFT JOIN base.cfgTblDealer as dealer "
 	    		//     + " WHERE base.dteDate between :dateFrom AND :dateTo "
	    		     + " GROUP BY dealer.txtCustomerCode,dealer.txtCustomerName,dealer.txtDisplayAddress,dealer.txtNtnNo,dealer.txtCnicNo"
	    		     + " ORDER BY dealer.txtCustomerCode " ;
 
//	    List<DashBoardDto> list = entityManager.createQuery(query).setParameter("dateFrom", dateFrom).setParameter("dateTo", dateTo).getResultList() ;
	    List<DashBoardRevenueDto> list = entityManager.createQuery(query).getResultList() ;
  	    entityManager.getTransaction().commit();
	    entityManager.close();
	    return list;
  	}
	
	
}
