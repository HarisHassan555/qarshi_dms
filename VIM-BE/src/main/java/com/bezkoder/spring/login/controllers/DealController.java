package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.SoapClientService;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.sa.bll.dto.DealRequest;
import com.bezkoder.spring.login.sa.bll.dto.SODTO;
import com.bezkoder.spring.login.sa.bll.dto.SODetailDTO;
import com.bezkoder.spring.login.sa.bll.services.ICityService;
import com.bezkoder.spring.login.sa.bll.services.ICustomerService;
import com.bezkoder.spring.login.sa.bll.services.IDealService;
import com.bezkoder.spring.login.sa.bll.services.IProductService;
import com.bezkoder.spring.login.sa.dal.entities.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;


@CrossOrigin( origins = "*" )
@RestController
//@RequestMapping("/api/deal")
public class DealController {

	private Logger logger = LogManager.getLogger(DealController.class);

	@Autowired
	private IDealService dealService;
	
	@Autowired
	private ICustomerService customerService;
	
	@Autowired
	private IProductService productService;
	
	
	@Autowired
	private ICommonService commonService;
	
	@Autowired
	private ICityService cityService;
	
	 @Autowired
	  private LoginDAO loginDao;


	@Autowired
	private SoapClientService soapClientService;
	

	@RequestMapping(value = "/getAllDeal", method = RequestMethod.GET)
	public List<SlsTblDeal> getAllDealAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDeales()");
		List<SlsTblDeal> deals = dealService.getAllDeal();
		return deals;
	}
	
	

	@RequestMapping(value = "/getActiveDeal", method = RequestMethod.GET)
	public List<SlsTblDeal> getActiveDeal(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDeales()");
		List<SlsTblDeal> deals = dealService.getActiveDeal();
		return deals;
	}

	@RequestMapping(value = "/searchDeal", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblDeal> searchDealAction(@RequestBody SlsTblDeal slsTblDeal) throws Exception {
		logger.debug("getAllDeales()");
		List<SlsTblDeal> deals = new ArrayList<>();
		try {

				/*DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
				LocalDate date = LocalDate.parse(slsTblDeal.getDte_date_from(), inputFormatter);
				DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String outputDate = date.format(outputFormatter);
				SOAPMessage soapResponse = soapClientService.sendSOAPRequest(outputDate);
				soapClientService.readSOAPResponse(soapResponse);*/
				deals = dealService.searchDeal(slsTblDeal);
			//return "SOAP Request Sent and Response Received!";
		} catch (Exception e) {
			e.printStackTrace();
		}

		return deals;
	}
	

	@RequestMapping(value = "/generateDealNo", method = RequestMethod.GET)
	public String generateDealNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return dealService.generateDealNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllDeal", method = RequestMethod.GET)
	public List<SlsTblDeal> getAllDeal(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDeal()");
		List<SlsTblDeal> deals = dealService.getAllDeal();
		return deals;
	}*/
	
	
	@RequestMapping(value = "/getNewDeal", method = RequestMethod.GET)
	public SlsTblDeal getNewDealAction(HttpServletRequest request, HttpServletResponse response) {
		SlsTblDeal deal = new SlsTblDeal();
		return deal;
	}
	
	/*@RequestMapping(value = "/addNewDeal", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProductComponent(@RequestBody SlsTblDeal slsTblDeal,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			if(slsTblDeal!=null)
			System.out.println("----componentList---------"+slsTblDeal);
			
			if(lstcfgTblProductComponents!=null && lstcfgTblProductComponents.size() >0)
			{
				return productComponentService.addNewProductComponentinList(lstcfgTblProductComponents);
			}
			
			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}*/

	@RequestMapping(value = "/addNewDeal", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewDealAction(@RequestBody SlsTblDeal citTblDeal, HttpServletRequest request,
			HttpServletResponse response) {
		try {
	
			return dealService.addNewDeal(citTblDeal);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteDeal", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteDealAction(@RequestBody String dealesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : dealesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return dealService.updateDeal(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	/*@RequestMapping(value = "/updateDeal", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDealAction(@RequestBody SlsTblDeal citTblDeal, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return dealService.updateDeal(citTblDeal);
		} catch (Exception ex) {
			return "Failure";
		}
	}*/
	
	
	@RequestMapping(value = "/updateDeal", method = RequestMethod.POST)
	public String updateDeal(@RequestParam(value = "so", required = false) String soString,@RequestParam(value = "soDetails", required = false) String soDetailsString
			,@RequestParam(value = "soSchedule", required = false) String soScheduleString,@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request, Model model)
			throws IllegalStateException, IOException {
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
			System.out.println("soDetailsString is------------" + soDetailsString);
			System.out.println("soScheduleString is------------" + soScheduleString);

			SlsTblDeal slsTblDeal = mapper.readValue(soString, SlsTblDeal.class);
			
//			SlsTblDealDetails[]  lstslsTblSoDetail = mapper.readValue(soDetailsString, SlsTblDealDetails[].class);
//			List<SlsTblDealDetails> listSODetail = Arrays.asList(lstslsTblSoDetail);
//			
//			
//			
//			
//			
//			
//		
//			slsTblDeal.setSlsTblDealDetails(listSODetail);
			
//			slsTblSoDetail.setSlsTblSaleItemSchedule(slsTblSaleItemSchedule);
			if (file != null && file.getBytes() != null) {
				
				
				System.out.println("file is------------" + file);
				slsTblDeal.setProfile_pic(file.getBytes());
				
				
				Encoder encoder = Base64.getUrlEncoder();
//				String originalinput = "https://stackabuse.com/tag/java/";
//				String encodedUrl = encoder.encodeToString(file.getBytes());
				slsTblDeal.setTxtImageName(file.getOriginalFilename());
				slsTblDeal.setTxtImageType(file.getContentType());
				String encodedUrl = Base64.getEncoder().encodeToString(file.getBytes());

//				System.out.println(encodedUrl);
			}

			return dealService.updateDeal(slsTblDeal);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/dealExistByProperty", method = RequestMethod.POST)
	public String dealExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return dealService.getDealByProperty(property, value, mode, oldValue)?"true":"false";
//					dealExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	

	
	
	@RequestMapping(value="dealBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("deal/bulk_deal");
		
			if (file.isEmpty()) {
				
				redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
	            
		    }
		 
		    String rootPath = request.getSession().getServletContext().getRealPath("/");
		    File dir = new File(rootPath + File.separator + "uploadedfile");
		    if (!dir.exists()) {
		        dir.mkdirs();
		    }
		 
		    File serverFile = new File(dir.getAbsolutePath() + File.separator + file.getOriginalFilename());
		
		   try {
		        try (InputStream is = file.getInputStream();
		                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile))) {
		            int i;
		            //write file to server
		            while ((i = is.read()) != -1) {
		                stream.write(i);
		            }
		            stream.flush();
		        }
		    } catch (IOException e) {
		    	 System.out.println("error while reading excel and put to db : " + e.getMessage());
		    }
		 
		   
		    try {
	
		    	FileInputStream excelFile = new FileInputStream(new File(serverFile.getPath().toString()));
	            Workbook workbook = new XSSFWorkbook(excelFile);
	            Sheet datatypeSheet = workbook.getSheetAt(0);
	            List<String[]> rows = new ArrayList<String[]>();
	            Row row;
	            for(int i=0; i<=datatypeSheet.getLastRowNum(); i++){
	            	 String[] tmpRows = new String[8];
	            	row = datatypeSheet.getRow(i);
	            	if(row.getRowNum()!=0){
	            	tmpRows[0]         =  row.getCell(0).getStringCellValue();
	            	tmpRows[1]      = row.getCell(1).getStringCellValue();
	            	tmpRows[2]        = row.getCell(2).getStringCellValue();
	            	tmpRows[3]   = row.getCell(3).getStringCellValue();
	            	int column4  =(int) row.getCell(4).getNumericCellValue();
	            	tmpRows[4]    = String.valueOf(column4);
	            	int column5=(int) row.getCell(5).getNumericCellValue();
	            	tmpRows[5]      = String.valueOf(column5);
	            	tmpRows[6]         = row.getCell(6).getStringCellValue();
	            	tmpRows[7]         = row.getCell(7).getStringCellValue();
	            	rows.add(tmpRows);
	            	}
	            	
	            }

	            ArrayList<String> errorRecord = null;
//	            		(ArrayList<String>) dealService.uploadFile(rows);
		        	if(errorRecord==null){
		        		modelMAP.addAttribute("msg", "Success");
		        	 return modelAndView;

		        	}
		        	else 
		        		//redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
		        	modelMAP.addAttribute("msg", errorRecord);
		        		 return modelAndView;
		        		 		//+ "&status="
		        	//+URLEncoder.encode(status,"UTF-8");
		       
		       
		    } catch (IOException e) {
		        System.out.println( e.getMessage());
		    }
		    return modelAndView;
			
		   
		
	}
	

	@RequestMapping(value = "/searchDealDetail", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblDealDetails> searchDealDetail(@RequestBody int dealesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllDeales()");
		List<SlsTblDealDetails> deals = dealService.searchDealDetail(dealesId);
	
		return deals;
  }
	
	
	
	
	@RequestMapping(value = "/searchDealDetailSchedule", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleItemSchedule> searchDealDetailSchedule(@RequestBody int dealesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchDealDetailSchedule()");
		List<SlsTblSaleItemSchedule> deals = dealService.searchDealDetailSchedule(dealesId,false);
	
		return deals;
  }
	
	@RequestMapping(value = "/searchDealDetailScheduleforDCForm", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleItemSchedule> searchDealDetailScheduleforDCForm(@RequestBody int dealesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchDealDetailSchedule()");
		List<SlsTblSaleItemSchedule> deals = dealService.searchDealDetailSchedule(dealesId,true);
	
		return deals;
  }
	
	

	
	
	
	@RequestMapping(value = "/updateDealfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDealfromSAP(@RequestBody SlsTblDeal slsTblDeal,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			
			if(slsTblDeal.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblDeal.getTxtSapNo());
				System.out.println(slsTblDeal.getTxtStatus());
				System.out.println(slsTblDeal.getTxtInvoiceStatus());
				System.out.println(slsTblDeal.getTxtDCStatus());
				System.out.println(slsTblDeal.getDteFinalApproval());
				System.out.println(slsTblDeal.getDteRSMApproval());
				
				return dealService.updateDealfromSAP(slsTblDeal);
			}
			else
				return "01-Sap No. is missing";
			
			

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	@RequestMapping(value = "/updateDealPricefromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDealPricefromSAP(@RequestBody SlsTblDeal slsTblDeal,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			
			if(slsTblDeal.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblDeal.getTxtSapNo());
				System.out.println(slsTblDeal.getTxtStatus());
				System.out.println(slsTblDeal.getTxtInvoiceStatus());
				System.out.println(slsTblDeal.getTxtDCStatus());
				System.out.println(slsTblDeal.getDteFinalApproval());
				System.out.println(slsTblDeal.getDteRSMApproval());
				
				return dealService.updateDealfromSAP(slsTblDeal);
			}
			else
				return "01-Sap No. is missing";
			
			

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	@RequestMapping(value = "/AddDealfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String AddDealfromSAP(@RequestBody SlsTblDeal slsTblDeal,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			
			if(slsTblDeal.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblDeal.getTxtSapNo());
				System.out.println(slsTblDeal.getTxtStatus());
				System.out.println(slsTblDeal.getTxtInvoiceStatus());
				System.out.println(slsTblDeal.getTxtDCStatus());
				System.out.println(slsTblDeal.getDteFinalApproval());
				System.out.println(slsTblDeal.getDteRSMApproval());
				
				return dealService.updateDealfromSAP(slsTblDeal);
			}
			else
				return "01-Sap No. is missing";
			
			

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	


	

	
	@RequestMapping(value = "/ApproveDeal", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveDeal(@RequestBody String dealesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			SlsTblDeal s1;
			for (String id : dealesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			
			}
			return dealService.updateDeal(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateDealValuefromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDealValuefromSAP(@RequestBody SlsTblDeal slsTblDeal,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("updateDealValuefromSAP()");
		
		try {
			
			if(slsTblDeal.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblDeal.getTxtSapNo());
				System.out.println(slsTblDeal.getTxtStatus());
				System.out.println(slsTblDeal.getTxtInvoiceStatus());
				System.out.println(slsTblDeal.getTxtDCStatus());
				System.out.println(slsTblDeal.getDteFinalApproval());
				System.out.println(slsTblDeal.getDteRSMApproval());
				slsTblDeal.setTxtMachineIp("netAmount");
				return dealService.updateDealfromSAP(slsTblDeal);
			}
			else
				return "01-Sap No. is missing";
			
			

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	
	@RequestMapping(value = "/AddNewDealfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String AddNewDealfromSAP(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		logger.debug("addNewBooking()");

		try {

			if (dto.getTxtOrderNO().trim().length() > 0) {

				SlsTblDeal slsTblDeal = new SlsTblDeal();
				slsTblDeal.setBlnFromSAP(true);
				slsTblDeal.setDteDate(dto.getDteDate());
				slsTblDeal.setTxtSapNo(dto.getTxtOrderNO());
				slsTblDeal.setTxtDealNo(dto.getTxtOrderNO());
				slsTblDeal.setTxtPONo(dto.getTxtPurchaseNo());

				if (dto.getTxtDealer() != null && dto.getTxtDealer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtDealer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblDeal.setCfgTblDealer(lstDealer.get(0));
					}
					else
					{
						cust.setTxtCustomerName(dto.getTxtDealerName());
						cust.setTxtSapNo(dto.getTxtDealer());
						cust.setTxtPhoneNo(dto.getTxtPhone());
						cust.setBlIsDealer(true);
						if(dto.getTxtCity()!=null && dto.getTxtCity().trim().length()>0)
						{
							CfgTblCity city = new CfgTblCity();
							city.setTxtCityName(dto.getTxtCity());
							List<CfgTblCity> lstCity = cityService.searchCity(city);
							if (lstCity != null && lstCity.size() > 0) {
								cust.setCfgTblCity(lstCity.get(0));
							}
						}
						cust = customerService.addNewCustomerFromSapOrder(cust);
						slsTblDeal.setCfgTblDealer(cust);
					}
				}

				if (dto.getTxtCustomer() != null && dto.getTxtCustomer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtCustomer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblDeal.setCfgTblCustomer(lstDealer.get(0));
					}
					else
					{
						cust.setTxtCustomerName(dto.getTxtCustomerName());
						cust.setTxtSapNo(dto.getTxtDealer());
						cust.setTxtPhoneNo(dto.getTxtPhone());
						cust.setBlIsDealer(false);
						if(dto.getTxtCity()!=null && dto.getTxtCity().trim().length()>0)
						{
							CfgTblCity city = new CfgTblCity();
							city.setTxtCityName(dto.getTxtCity());
							List<CfgTblCity> lstCity = cityService.searchCity(city);
							if (lstCity != null && lstCity.size() > 0) {
								cust.setCfgTblCity(lstCity.get(0));
							}
						}
						cust = customerService.addNewCustomerFromSapOrder(cust);
						slsTblDeal.setCfgTblCustomer(cust);
					}
				}

				if (dto.getTxtItemNo() != null && dto.getTxtItemNo().trim().length() > 0) {
					CfgTblProduct prd = new CfgTblProduct();
					prd.setTxtProductCode(dto.getTxtItemNo());
					List<CfgTblProduct> lstProduct = productService.searchProduct(prd);
					List lstDetail = new ArrayList();
					if (lstProduct != null && lstProduct.size() > 0) {
						slsTblDeal.setCfgTblProduct(lstProduct.get(0));
						slsTblDeal.setNumQuantity(dto.getNumQty());

						SlsTblDealDetails detail = new SlsTblDealDetails();
						detail.setCfgTblProduct(lstProduct.get(0));
						detail.setNumQuantity(dto.getNumQty());
						List<SlsTblSaleItemSchedule> listSchedule = new ArrayList();

						if (dto.getLstSchedule() != null && dto.getLstSchedule().size() > 0) {
							SlsTblSaleItemSchedule sch;
							for (SODetailDTO schedule : dto.getLstSchedule()) {
								sch = new SlsTblSaleItemSchedule();
								sch.setNumQuantity(schedule.getNumQty());
								sch.setDteDate(schedule.getDteDate());
								listSchedule.add(sch);
							}
						}
						detail.setSlsTblSaleItemSchedule(listSchedule);
						lstDetail.add(detail);
						slsTblDeal.setSlsTblDealDetails(lstDetail);
					}

				}

				return dealService.addNewDeal(slsTblDeal);

			} else
				return "01-Sap No. is missing";

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	
	@RequestMapping(value = "/UpdateNewDealfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String UpdateNewDealfromSAP(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		logger.debug("addNewBooking()");

		try {
			
			SlsTblDeal slsOrder=new SlsTblDeal();
			if (dto.getTxtOrderNO().trim().length() > 0) {
				slsOrder.setTxtSapNo(dto.getTxtOrderNO());
				List<SlsTblDeal> lstOrder = dealService.searchDeal(slsOrder);
				SlsTblDeal slsTblDeal = new SlsTblDeal();
				if(lstOrder!=null && lstOrder.size() >0)
				{
					slsTblDeal=lstOrder.get(0);
				}
				else return "Order Not found";
				
				slsTblDeal.setBlnFromSAP(true);
				slsTblDeal.setDteDate(dto.getDteDate());
				slsTblDeal.setTxtSapNo(dto.getTxtOrderNO());
				slsTblDeal.setTxtDealNo(dto.getTxtOrderNO());
				slsTblDeal.setTxtPONo(dto.getTxtPurchaseNo());

				if (dto.getTxtDealer() != null && dto.getTxtDealer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtDealer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblDeal.setCfgTblDealer(lstDealer.get(0));
					}
					else
					{
						cust.setTxtCustomerName(dto.getTxtDealerName());
						cust.setTxtSapNo(dto.getTxtDealer());
						cust.setTxtPhoneNo(dto.getTxtPhone());
						cust.setBlIsDealer(true);
						if(dto.getTxtCity()!=null && dto.getTxtCity().trim().length()>0)
						{
							CfgTblCity city = new CfgTblCity();
							city.setTxtCityName(dto.getTxtCity());
							List<CfgTblCity> lstCity = cityService.searchCity(city);
							if (lstCity != null && lstCity.size() > 0) {
								cust.setCfgTblCity(lstCity.get(0));
							}
						}
						cust = customerService.addNewCustomerFromSapOrder(cust);
						slsTblDeal.setCfgTblDealer(cust);
					}
				}

				if (dto.getTxtCustomer() != null && dto.getTxtCustomer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtCustomer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblDeal.setCfgTblCustomer(lstDealer.get(0));
					}
					else
					{
						cust.setTxtCustomerName(dto.getTxtCustomerName());
						cust.setTxtSapNo(dto.getTxtDealer());
						cust.setTxtPhoneNo(dto.getTxtPhone());
						cust.setBlIsDealer(false);
						if(dto.getTxtCity()!=null && dto.getTxtCity().trim().length()>0)
						{
							CfgTblCity city = new CfgTblCity();
							city.setTxtCityName(dto.getTxtCity());
							List<CfgTblCity> lstCity = cityService.searchCity(city);
							if (lstCity != null && lstCity.size() > 0) {
								cust.setCfgTblCity(lstCity.get(0));
							}
						}
						cust = customerService.addNewCustomerFromSapOrder(cust);
						slsTblDeal.setCfgTblCustomer(cust);
					}
				}

				if (dto.getTxtItemNo() != null && dto.getTxtItemNo().trim().length() > 0) {
					CfgTblProduct prd = new CfgTblProduct();
					prd.setTxtProductCode(dto.getTxtItemNo());
					List<CfgTblProduct> lstProduct = productService.searchProduct(prd);
					List lstDetail = new ArrayList();
					if (lstProduct != null && lstProduct.size() > 0) {
						SlsTblDealDetails detail = new SlsTblDealDetails();
						List<SlsTblDealDetails> deals = dealService.searchDealDetail(slsTblDeal.getSerDealId());
						if(deals !=null && deals.size() >0)
							detail=deals.get(0);
						
						slsTblDeal.setCfgTblProduct(lstProduct.get(0));
						slsTblDeal.setNumQuantity(dto.getNumQty());

						
						detail.setCfgTblProduct(lstProduct.get(0));
						detail.setNumQuantity(dto.getNumQty());
						List<SlsTblSaleItemSchedule> listSchedule = new ArrayList();

						if (dto.getLstSchedule() != null && dto.getLstSchedule().size() > 0) {
							SlsTblSaleItemSchedule sch;
							for (SODetailDTO schedule : dto.getLstSchedule()) {
								sch = new SlsTblSaleItemSchedule();
								sch.setNumQuantity(schedule.getNumQty());
								sch.setDteDate(schedule.getDteDate());
								listSchedule.add(sch);
							}
						}
						detail.setSlsTblSaleItemSchedule(listSchedule);
						lstDetail.add(detail);
						slsTblDeal.setSlsTblDealDetails(lstDetail);
					}

				}

				return dealService.updateDeal(slsTblDeal);

			} else
				return "01-Sap No. is missing";

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}


	@RequestMapping(value = "/closeDeal", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String closeDeal(@RequestBody DealRequest dealesId, HttpServletRequest request,
							HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			/*SlsTblDeal s1;
			for (String id : dealesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}

			}*/
			idList.add(dealesId.getId());
			return dealService.updateDeal(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

}
