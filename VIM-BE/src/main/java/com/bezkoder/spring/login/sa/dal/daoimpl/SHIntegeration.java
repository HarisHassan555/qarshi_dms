package com.bezkoder.spring.login.sa.dal.daoimpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import com.bezkoder.spring.login.admin.ServerConfiguration;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.sa.bll.dto.CustomerDTO;
import com.bezkoder.spring.login.sa.bll.dto.PaymentDTO;
import com.bezkoder.spring.login.sa.bll.dto.SOIntegerationDTO;
import com.bezkoder.spring.login.sa.bll.dto.SOIntegerationDetailDTO;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblCustomer;
import com.bezkoder.spring.login.sa.dal.entities.CfgTblProduct;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSaleOrder;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoDetail;
import com.bezkoder.spring.login.sa.dal.entities.SlsTblSoPayments;
import com.bezkoder.spring.login.util.UtilDateAndTime;
import com.bezkoder.spring.login.util.UtilRestTemplate;

@Repository
public class SHIntegeration {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	public List<SlsTblSoDetail> searchSaleOrderDetail(int SaleOrderId) {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();
		String query = "";
		if (SaleOrderId > 0)
			query = "from SlsTblSoDetail ff  where (blIsDeleted is null or blIsDeleted=FALSE ) and ff.slsTblSaleOrder.serSaleOrderId= "
					+ SaleOrderId;

		else
			return null;

		System.out.println("query ----:" + query.substring(0, query.length()));
		String subQuery = query.substring(0, query.length());
		List<SlsTblSoDetail> lstSOD = entityManager.createQuery(subQuery).getResultList();

		entityManager.getTransaction().commit();
		entityManager.close();

		return lstSOD;
	}

	public HttpHeaders getHttpHeadersForTocken() {
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_JSON);
		return header;
	}

	public String getAuthTocken() {
		String accessFinancialPath = "";
		CustomerDTO dto = new CustomerDTO();
		dto.setEmail("admin@sidat.com.pk");
		dto.setPassword("Sidat@123");

		Map<String, String> requestParams = new HashMap<String, String>();
		ResponseEntity<String> doPostRestTemplateCall = UtilRestTemplate
				.doPost(ServerConfiguration.service_auth_tokenSH, dto, getHttpHeadersForTocken(), requestParams);

		try {
			System.out.println(doPostRestTemplateCall.getStatusCode());
			System.out.println(doPostRestTemplateCall.getStatusCodeValue());

			JSONObject obj = new JSONObject(doPostRestTemplateCall.getBody());
			System.out.println(obj.get("value"));
			return obj.get("value").toString();

		} catch (JSONException e) {
			e.printStackTrace();
//			LOGGER.error(e);
		}
		return "NF";
	}

	public HttpHeaders getHttpHeaders() {
		HttpHeaders header = new HttpHeaders();
		header.add("Authorization", "Bearer " + getAuthTocken());
		header.setContentType(MediaType.APPLICATION_JSON);
		return header;
	}

	public String CreateCustomerInSHERP(CfgTblCustomer customer,CfgTblCustomer dealer) {
		
		if(ServerConfiguration.integeration_required)
		{
		if(dealer.getCfgTblCustomerCategory().getTxtCustomerCategoryName().indexOf("GAL")>=0)
		{
		String accessFinancialPath = "";
		CustomerDTO dto = new CustomerDTO();
		dto.setCst_Code(customer.getTxtCustomerCode());
		dto.setCst_Name(customer.getTxtCustomerName());
		dto.setCst_short_name(customer.getTxtCustomerName());

		

		
		if(dealer!=null && dealer.getTxtGstNameOnInvoice()!=null && dealer.getTxtGstNameOnInvoice().trim().length() ==6)
			dto.setCsttyp_Code(dealer.getTxtGstNameOnInvoice().trim());
		else
			dto.setCsttyp_Code("000008");
		
		
		dto.setCty_Code("0"+customer.getCfgTblCity().getTxtCityCode());

//		dto.setCstg_Code(customer.getCfgTblZone1().getTxtZone1Code());
//		dto.setBsg2_Code(customer.getCfgTblZone2().getTxtZone2Code());
		dto.setCstg_Code(dealer.getTxtCustomerCode());
		dto.setBsg2_Code("000001");

		dto.setSg1_Code(customer.getCfgTblZone3().getTxtZone3Code());

		if (customer.getTxtMobileNo() != null && customer.getTxtMobileNo().trim().length() > 0)
			dto.setCst_phone(customer.getTxtMobileNo());
		else if (customer.getTxtPhoneNo() != null && customer.getTxtPhoneNo().trim().length() > 0)
			dto.setCst_phone(customer.getTxtPhoneNo());
		else if (customer.getTxtPhoneNo2() != null && customer.getTxtPhoneNo2().trim().length() > 0)
			dto.setCst_phone(customer.getTxtPhoneNo2());
		else
			dto.setCst_phone("042344321");

		if (customer.getTxtEmailAddress() != null && customer.getTxtEmailAddress().trim().length() > 0)
			dto.setCst_Email(customer.getTxtEmailAddress());
		else
			dto.setCst_Email("");

		if (customer.getTxtNtnNo() != null && customer.getTxtNtnNo().trim().length() > 0)
			dto.setCst_NTN_No(customer.getTxtNtnNo());
		else
			dto.setCst_NTN_No("");

		if (customer.getTxtGstNumber() != null && customer.getTxtGstNumber().trim().length() > 0)
			dto.setCst_GSTNo(customer.getTxtGstNumber());
		else
			dto.setCst_GSTNo("");
		
		if (customer.getTxtDisplayAddress() != null && customer.getTxtDisplayAddress().trim().length() > 0)
			dto.setCst_address1(customer.getTxtDisplayAddress());
		else
			dto.setCst_address1("NA");
		
		if (customer.getTxtBillingAddress() != null && customer.getTxtBillingAddress().trim().length() > 0)
			dto.setCst_address2(customer.getTxtBillingAddress());
		else
			dto.setCst_address2("NA");
		
		
		if (customer.getTxtShippingAddress() != null && customer.getTxtShippingAddress().trim().length() > 0)
			dto.setCst_address3(customer.getTxtShippingAddress());
		else
			dto.setCst_address3("NA");
		
		
		if(customer.getBlnIsFiler()!=null && customer.getBlnIsFiler())
		{
			dto.setAdv_filertag(1);
		}
		else
			dto.setAdv_filertag(0);
		

		customer.setTxtXMSent(dto.toString());
		Map<String, String> requestParams = new HashMap<String, String>();
		ResponseEntity<String> doPostRestTemplateCall = UtilRestTemplate.doPost(
				 ServerConfiguration.service_customerSH, dto, getHttpHeaders(),
//				accessFinancialPath + "http://115.167.64.214:5000/SCM/CreateCustomer", dto, getHttpHeaders(),
				requestParams);
		doPostRestTemplateCall.getStatusCode();
		try {
			JSONObject obj = new JSONObject(doPostRestTemplateCall.getBody());
			try {
				System.out.println(doPostRestTemplateCall.getStatusCode());
				System.out.println(doPostRestTemplateCall.getStatusCodeValue());

				System.out.println(obj.get("description") + "-" + obj.get("status"));

				return obj.get("description") + "-" + obj.get("status");
			} catch (JSONException e) {
				e.printStackTrace();
//				LOGGER.error(e);
			}
		} catch (JSONException e) {
			e.printStackTrace();
//			LOGGER.error(e);
		}
		return "error";
		}
		else
			return "No Integeration For GDF ";
		}
		else
		{
		 return	"Save Successfully.";
		}
	}

	public String CreatePaymentInSHERP(SlsTblSoPayments slsTblSoPayments) {
		if(ServerConfiguration.integeration_required)
		{
		if (slsTblSoPayments.getSlsTblSaleOrder().getBlIsGAL()) {
			String accessFinancialPath = "";
			PaymentDTO dto = new PaymentDTO();

//	"003589");
			dto.setCst_Code(slsTblSoPayments.getSlsTblSaleOrder().getCfgTblDealer().getTxtCustomerCode());
			dto.setRecm_Date(UtilDateAndTime.dateToStringyyyymmdd(slsTblSoPayments.getDteDate()));
			if (slsTblSoPayments.getBlIsAdvance() != null && slsTblSoPayments.getBlIsAdvance())
				dto.setRecm_AdvancePay(1);
			else
				dto.setRecm_AdvancePay(0);

			if (slsTblSoPayments.getCfgTblBank() != null)
				dto.setBnk_code(slsTblSoPayments.getCfgTblBank().getTxtBankCode().trim());
			else
				dto.setBnk_code("");

			if (slsTblSoPayments.getTxtChequeNo() != null && slsTblSoPayments.getTxtChequeNo().trim().length() > 0)
				dto.setRecm_chqno(slsTblSoPayments.getTxtChequeNo().trim());
			else
				dto.setRecm_chqno("");

			if (slsTblSoPayments.getTxtRemarks() != null && slsTblSoPayments.getTxtRemarks().trim().length() > 0)
				dto.setRecm_narration(slsTblSoPayments.getTxtRemarks().trim());
			else
				dto.setRecm_narration("");

//		
//		<option value="">--Select--</option>
//		<option value="Online Transfer">Online Transfer</option>
//		<option value="Cheque">Cheque</option>
//		<option value="Cash">Cash</option>
//		<option value="Pay Order">Pay Order</option>
//		<option value="Demand Draft">Demand Draft</option>
//		<option value="L.C">L.C</option>

			if (slsTblSoPayments.getTxtPaymentMethod() != null
					&& slsTblSoPayments.getTxtPaymentMethod().trim().equalsIgnoreCase("L.C"))
				dto.setPaymod_Type("000000000000001");
			else if (slsTblSoPayments.getTxtPaymentMethod() != null
					&& slsTblSoPayments.getTxtPaymentMethod().trim().equalsIgnoreCase("Cash"))
				dto.setPaymod_Type("000000000000002");
			else if (slsTblSoPayments.getTxtPaymentMethod() != null
					&& slsTblSoPayments.getTxtPaymentMethod().trim().equalsIgnoreCase("Cheque"))
				dto.setPaymod_Type("000000000000004");
			else if (slsTblSoPayments.getTxtPaymentMethod() != null
					&& slsTblSoPayments.getTxtPaymentMethod().trim().equalsIgnoreCase("Online Transfer"))
				dto.setPaymod_Type("000000000000005");
			else if (slsTblSoPayments.getTxtPaymentMethod() != null
					&& slsTblSoPayments.getTxtPaymentMethod().trim().equalsIgnoreCase("Pay Order"))
				dto.setPaymod_Type("000000000000006");
			else if (slsTblSoPayments.getTxtPaymentMethod() != null
					&& slsTblSoPayments.getTxtPaymentMethod().trim().equalsIgnoreCase("Demand Draft"))
				dto.setPaymod_Type("000000000000006");

			dto.setRecm_TotalAmount(slsTblSoPayments.getNumPaymentReceived());
			dto.setRecm_WHTaxYesNo(0);
			try {
				if (slsTblSoPayments.getTxtWithheld() != null
						&& slsTblSoPayments.getTxtWithheld().trim().length() > 0) {
					dto.setRecm_WHTaxYesNo(1);
					dto.setRecm_TotalWHTaxAmt(new BigDecimal(slsTblSoPayments.getTxtWithheld().trim()));
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			if (slsTblSoPayments.getNumST() != null && slsTblSoPayments.getNumST().doubleValue() > 0) {
				dto.setRecm_stax_detctn(slsTblSoPayments.getNumST());
			}

			dto.setDept_code("050005");

			if (slsTblSoPayments.getSlsTblSaleOrder().getBlIsSales()) {
				dto.setDept_code("050005");
//			  dto.setsO_DocType("000000000000003");
				try {
					if (slsTblSoPayments.getSlsTblSaleOrder().getBlIsGAL()) {
						if (slsTblSoPayments.getSlsTblSaleOrder().getSlsTblSoDetails().get(0).getCfgTblProduct()
								.getTxtProductName().toUpperCase().indexOf("CHERY") > 0) {
							dto.setDept_code("050005");
//								 dto.setsO_DocType("000000000000151");
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
//			  dto.setsO_DocType("000000000000107");
				dto.setDept_code("070001");
			}

			slsTblSoPayments.setTxtXMSent(dto.toString());
			Map<String, String> requestParams = new HashMap<String, String>();
			ResponseEntity<String> doPostRestTemplateCall = UtilRestTemplate
					.doPost(ServerConfiguration.service_paymentSH, dto, getHttpHeaders(),
//				accessFinancialPath + "http://115.167.64.214:5000/SCM/CreateCustomer", dto, getHttpHeaders(),
							requestParams);
			doPostRestTemplateCall.getStatusCode();
			try {
				JSONObject obj = new JSONObject(doPostRestTemplateCall.getBody());
				try {
					System.out.println(doPostRestTemplateCall.getStatusCode());
					System.out.println(doPostRestTemplateCall.getStatusCodeValue());

					System.out.println(obj.get("description") + "-" + obj.get("status"));

					return obj.get("description") + "-" + obj.get("status");
				} catch (JSONException e) {
					e.printStackTrace();
//				LOGGER.error(e);
				}
			} catch (JSONException e) {
				e.printStackTrace();
//			LOGGER.error(e);
			}
			return "error";
		} else
			return "No Integeration For GDF";
		}
		else
		{
		 return	"Save Successfully.";
		}
	}

	public String CreateSOInSHERP(SlsTblSaleOrder saleOrder, List<SlsTblSoDetail> lstDetail) {
		
		if(ServerConfiguration.integeration_required)
		{
		if (saleOrder.getBlIsGAL() != null && saleOrder.getBlIsGAL()) {
			String accessFinancialPath = "";
			SOIntegerationDTO dto = new SOIntegerationDTO();

//		dto.setCst_Code("003569");
			
			
			if (saleOrder.getBlIsSales())
			{
			if (saleOrder.getCfgTblDealer() != null)
				dto.setCst_Code(saleOrder.getCfgTblDealer().getTxtCustomerCode());
			else {
				if (saleOrder.getCfgTblDealerOne() != null)
					dto.setCst_Code(saleOrder.getCfgTblDealerOne().getTxtCustomerCode());
			}
			}
			else
				dto.setCst_Code("001361");
			
//				slsTblSoPayments.getSlsTblSaleOrder().getCfgTblDealer().getTxtCustomerCode());
			dto.setSom_Date(UtilDateAndTime.dateToStringyyyymmdd(saleOrder.getDteDate()));

			if (saleOrder.getTxtColor() != null && saleOrder.getTxtColor().trim().length() > 0)
				dto.setColor(saleOrder.getTxtColor());
			else
				dto.setColor("");

			dto.setsO_DocNumber(saleOrder.getTxtSaleOrderNo());

			dto.setDept_code("050005");
			dto.setsO_DocType("000000000000003");
			/**
			 * For GDF Departments Code Description 050001 MARKETING & SALES 070001 PARTS
			 * DEPTT.
			 * 
			 * FOR GAL List of Departments Code Description 050001 MARKETING & SALES 050005
			 * MKT CHERY 060002 SERVICE DEPTT. 070001 PARTS DEPTT.
			 * 
			 * 
			 */

			/**
			 * For GDF Documents mapped with departments Document Type Code Sales Order
			 * 000000000000003 Sales Order- Chery 000000000000151 Sales Order Service
			 * 000000000000118 SALES ORDER-PARTS 000000000000107
			 * 
			 * FOR GAL Documents Type Documents Type Code Sales Order 000000000000003 SALES
			 * ORDER- PARTS 000000000000107
			 * 
			 * 
			 * 
			 */

			if (saleOrder.getBlIsSales()) {
				dto.setDept_code("050005");
				dto.setsO_DocType("000000000000003");
				try {
					if (saleOrder.getBlIsGAL()) {
						if (lstDetail.get(0).getCfgTblProduct().getTxtProductName().toUpperCase()
								.indexOf("CHERY") > 0) {
							dto.setDept_code("050005");
							dto.setsO_DocType("000000000000151");
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				dto.setsO_DocType("000000000000107");
				dto.setDept_code("070001");
			}

			SlsTblSoDetail soDetail;
//		List<SlsTblSoDetail> lstDetailnew = searchSaleOrderDetail(saleOrder.getSerSaleOrderId());

			Iterator<SlsTblSoDetail> itr = lstDetail.iterator();
			SlsTblSoDetail details = new SlsTblSoDetail();
			CfgTblProduct product = new CfgTblProduct();
			double order_Qty = 0;
			List<SOIntegerationDetailDTO> lstDetail2 = new ArrayList();
			while (itr.hasNext()) {
				soDetail = (SlsTblSoDetail) itr.next();
				details = soDetail;
				SOIntegerationDetailDTO detailDTO = new SOIntegerationDetailDTO();
				if (saleOrder.getBlIsSales())
				{
				if(details.getCfgTblProduct().getTxtProductCode().length() == 11 )
					detailDTO.setItm_ItemCode(details.getCfgTblProduct().getTxtProductCode());
				else
					detailDTO.setItm_ItemCode("45110100001");
				}
				else
				{
					detailDTO.setItm_ItemCode(details.getCfgTblProduct().getTxtProductCode());
				}

//				detailDTO.setSod_SellingQty(new BigDecimal(1.0));
//
//				detailDTO.setSod_Qty(new BigDecimal(1.0));
//
//				detailDTO.setSod_QtyRemain(new BigDecimal(1.0));
				
				
				detailDTO.setSod_SellingQty(details.getNumQuantity());

				detailDTO.setSod_Qty(details.getNumQuantity());

				detailDTO.setSod_QtyRemain(details.getNumQuantity());
				

				detailDTO.setSod_SellingUnitCode("0002");

				detailDTO.setSod_Conversion(new BigDecimal(1.0));
				detailDTO.setSod_StoringUnitCode("0002");

				detailDTO.setSod_Rate(details.getNumItemPrice());
				lstDetail2.add(detailDTO);
			}

			dto.setSoDetail(lstDetail2);

			saleOrder.setTxtXMSent(dto.toString());
			Map<String, String> requestParams = new HashMap<String, String>();
			ResponseEntity<String> doPostRestTemplateCall = UtilRestTemplate.doPost(ServerConfiguration.service_SOSH,
					dto, getHttpHeaders(), requestParams);
			doPostRestTemplateCall.getStatusCode();
			try {
				JSONObject obj = new JSONObject(doPostRestTemplateCall.getBody());
				try {
					System.out.println(doPostRestTemplateCall.getStatusCode());
					System.out.println(doPostRestTemplateCall.getStatusCodeValue());

					System.out.println(obj.get("description") + "-" + obj.get("status"));

					return obj.get("description") + "-" + obj.get("status");
				} catch (JSONException e) {
					e.printStackTrace();
//				LOGGER.error(e);
				}
			} catch (JSONException e) {
				e.printStackTrace();
//			LOGGER.error(e);
			}
			return "error";
		} else
			return "No Integeration For GDF ";
	}
	
	else
	{
	 return	"Save Successfully.";
	}
	}
}
