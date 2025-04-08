package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.ServerConfiguration;
import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.UserService;
import com.bezkoder.spring.login.admin.dal.dao.ICfgTblUserDAO;
import com.bezkoder.spring.login.admin.dal.dao.LoginDAO;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.sa.bll.dto.ReportDTO;
import com.bezkoder.spring.login.sa.bll.dto.SODTO;
import com.bezkoder.spring.login.sa.bll.dto.SODetailDTO;
import com.bezkoder.spring.login.sa.bll.dto.ServiceDTO;
import com.bezkoder.spring.login.sa.bll.services.ICityService;
import com.bezkoder.spring.login.sa.bll.services.ICustomerService;
import com.bezkoder.spring.login.sa.bll.services.IProductService;
import com.bezkoder.spring.login.sa.bll.services.ISaleOrderService;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSaleOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.stream.Collectors;

;


@RestController
@CrossOrigin(origins = "*" )
public class SaleOrderController {

	private Logger logger = LogManager.getLogger(SaleOrderController.class);

	@Autowired
	private ISaleOrderService saleOrderService;
	
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
	  private  ISlsTblSaleOrderDAO salesDAO;


	@Autowired
	private UserService userService;


	@Autowired
	private ICfgTblUserDAO citTableUserDAO;

	@RequestMapping(value = "/getAllSaleOrder", method = RequestMethod.GET)
	public List<SlsTblSaleOrder> getAllSaleOrderAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderes()");
		List<SlsTblSaleOrder> saleOrders = saleOrderService.getAllSaleOrder();
		return saleOrders;
	}
	
	
	@RequestMapping(value="/searchSaleOrder",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleOrder> searchSaleOrderAction(@RequestBody SlsTblSaleOrder slsTblSaleOrder,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderes()");
		List<SlsTblSaleOrder> saleOrders = saleOrderService.searchSaleOrder(slsTblSaleOrder);
		return saleOrders;
		
	}
	
	@RequestMapping(value="/generateSaleOrderNoForDealer",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String generateSaleOrderNoForDealer(@RequestBody CfgTblCustomer cfgTblCustomer,HttpServletRequest request, HttpServletResponse response) {
		try {
			return saleOrderService.generateSaleOrderNo(cfgTblCustomer.getTxtEmailAddress());
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
		
	}
	
	@RequestMapping(value="/generateSaleOrderNoForDealerGNL",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String generateSaleOrderNoForDealerGNL(@RequestBody SlsTblSaleOrder SlsTblSaleOrder,HttpServletRequest request, HttpServletResponse response) {
		try {
			return salesDAO.generateSaleOrderNoFORGNL(SlsTblSaleOrder);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
		
	}
	

	@RequestMapping(value = "/generateSaleOrderNo", method = RequestMethod.GET)
	public String generateSaleOrderNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return saleOrderService.generateSaleOrderNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllSaleOrder", method = RequestMethod.GET)
	public List<SlsTblSaleOrder> getAllSaleOrder(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrder()");
		List<SlsTblSaleOrder> saleOrders = saleOrderService.getAllSaleOrder();
		return saleOrders;
	}*/
	
	
	@RequestMapping(value = "/getNewSaleOrder", method = RequestMethod.GET)
	public SlsTblSaleOrder getNewSaleOrderAction(HttpServletRequest request, HttpServletResponse response) {
		SlsTblSaleOrder saleOrder = new SlsTblSaleOrder();
		return saleOrder;
	}
	
	/*@RequestMapping(value = "/addNewSaleOrder", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProductComponent(@RequestBody SlsTblSaleOrder slsTblSaleOrder,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			if(slsTblSaleOrder!=null)
			System.out.println("----componentList---------"+slsTblSaleOrder);
			
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

	@RequestMapping(value = "/addNewSaleOrder", method = RequestMethod.POST)
//	public String addNewSaleOrderAction(@RequestBody SlsTblSaleOrder citTblSaleOrder, @RequestParam(value = "newDocument") String candidateDocumentInfo,
	public String addNewSaleOrderAction(@RequestParam(value = "so", required = false) String soString, @RequestParam(value = "newDocument", required = false) String candidateDocumentInfo,
			@RequestParam(value = "file", required = false) MultipartFile file,HttpServletRequest request,
			HttpServletResponse response) {
		try {

	ObjectMapper mapper = new ObjectMapper();
	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	


	SlsTblSaleOrder slsTblSaleOrder = mapper.readValue(soString, SlsTblSaleOrder.class);
	
	List<SlsTblSoPayments> lstPayment=new ArrayList();
	List<SlsTblSoPayments> lstPaymentREC=new ArrayList();
	
	if(slsTblSaleOrder.getTxtChequeNo()!=null && slsTblSaleOrder.getTxtChequeNo().trim().length() > 0)
	{
		SlsTblSoPayments slsTblSoPayments = new SlsTblSoPayments();
		slsTblSoPayments.setTxtChequeNo(slsTblSaleOrder.getTxtChequeNo());
		lstPayment= salesDAO.CheckPaymentDuplicationChequeNo(slsTblSoPayments);
	}
	
	if(slsTblSaleOrder.getTxtSlipNo()!=null && slsTblSaleOrder.getTxtSlipNo().trim().length() > 0)
	{
		SlsTblSoPayments slsTblSoPayments = new SlsTblSoPayments();
		slsTblSoPayments.setTxtSlipNo(slsTblSaleOrder.getTxtSlipNo());
		lstPaymentREC= salesDAO.CheckPaymentDuplicationtxtSlipNo(slsTblSoPayments);
	}
	

	
	if(lstPayment!=null && lstPayment.size() >0)
	{
		if(lstPaymentREC!=null && lstPaymentREC.size() >0)
		{
			return "DCH&SLIP";
		}
		else
			return "DCH";
	}
	else if(lstPaymentREC!=null && lstPaymentREC.size() >0)
	{
		return "DSLIP";
	}
	
	SOPaymentDocument paymentDocument=new SOPaymentDocument();
	if(candidateDocumentInfo != null && !(candidateDocumentInfo.equalsIgnoreCase("undefined")))
	 paymentDocument =mapper.readValue(candidateDocumentInfo,SOPaymentDocument.class);
	if(file !=null )
	paymentDocument.setDocumentFile(file.getBytes());
	return saleOrderService.addNewSaleOrderWithPaymentDocument(slsTblSaleOrder,paymentDocument);
//			return saleOrderService.addNewSaleOrder(slsTblSaleOrder);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@Autowired ISlsTblSaleOrderDAO sodao;

	@RequestMapping(value = "/deleteSaleOrder", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteSaleOrderAction(@RequestBody String saleOrderesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : saleOrderesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return saleOrderService.updateSaleOrder(idList,null);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}
	
	
	

	/*@RequestMapping(value = "/updateSaleOrder", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSaleOrderAction(@RequestBody SlsTblSaleOrder citTblSaleOrder, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return saleOrderService.updateSaleOrder(citTblSaleOrder);
		} catch (Exception ex) {
			return "Failure";
		}
	}*/
	
	
	@RequestMapping(value = "/updateSaleOrder", method = RequestMethod.POST)
	public String updateSaleOrder(@RequestParam(value = "so", required = false) String soString,@RequestParam(value = "soDetails", required = false) String soDetailsString
			,@RequestParam(value = "soSchedule", required = false) String soScheduleString,@RequestParam(value = "file", required = false) MultipartFile file, HttpServletRequest request, Model model)
			throws IllegalStateException, IOException {
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
			System.out.println("soDetailsString is------------" + soDetailsString);
			System.out.println("soScheduleString is------------" + soScheduleString);

			SlsTblSaleOrder slsTblSaleOrder = mapper.readValue(soString, SlsTblSaleOrder.class);
			
			SlsTblSoDetail[]  lstslsTblSoDetail = null;
				if(soDetailsString!=null && soDetailsString.length() > 2)
					lstslsTblSoDetail=mapper.readValue(soDetailsString, SlsTblSoDetail[].class);
			List<SlsTblSoDetail> listSODetail = new ArrayList();
			if(lstslsTblSoDetail!= null)
					listSODetail=Arrays.asList(lstslsTblSoDetail);
			
			List<SlsTblSaleItemSchedule> listSchedule =new ArrayList();
//			if(soScheduleString.trim().length()>5)
//			{
//				SlsTblSaleItemSchedule[]  lstslsTblSaleItemSchedule = mapper.readValue(soScheduleString, SlsTblSaleItemSchedule[].class);
//				
//				listSchedule = Arrays.asList(lstslsTblSaleItemSchedule);
//			}
//			else
//			{
//				double total_qty=0;
//				List<SlsTblSaleItemSchedule> lstSchedule= saleOrderService.searchSaleOrderDetailSchedule(listSODetail.get(0).getSerSoDetailId(),false);
//				if(lstSchedule!=null && lstSchedule.size() >0)
//				{
//					for(SlsTblSaleItemSchedule dto :lstSchedule )
//					{
//						total_qty = total_qty + dto.getNumQuantity().doubleValue();
//					}
//					
//					if(!(total_qty == listSODetail.get(0).getNumQuantity().doubleValue()))
//					{
//						return "SQNC";  //Schedule Quantity not correct
//					}
//				}
//			}
			
			
			
			
		
//			listSODetail.get(0).setSlsTblSaleItemSchedule(listSchedule);
			slsTblSaleOrder.setSlsTblSoDetails(listSODetail);
			
//			slsTblSoDetail.setSlsTblSaleItemSchedule(slsTblSaleItemSchedule);
			if (file != null && file.getBytes() != null) {
				
				
				System.out.println("file is------------" + file);
				slsTblSaleOrder.setProfile_pic(file.getBytes());
				
				
				Encoder encoder = Base64.getUrlEncoder();
//				String originalinput = "https://stackabuse.com/tag/java/";
//				String encodedUrl = encoder.encodeToString(file.getBytes());
				slsTblSaleOrder.setTxtImageName(file.getOriginalFilename());
				slsTblSaleOrder.setTxtImageType(file.getContentType());
				String encodedUrl = Base64.getEncoder().encodeToString(file.getBytes());

//				System.out.println(encodedUrl);
			}

			return saleOrderService.updateSaleOrder(slsTblSaleOrder);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/saleOrderExistByProperty", method = RequestMethod.POST)
	public String saleOrderExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return saleOrderService.getSaleOrderByProperty(property, value, mode, oldValue)?"true":"false";
//					saleOrderExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	

	
	
	@RequestMapping(value="saleOrderBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("saleOrder/bulk_saleOrder");
		
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
//	            		(ArrayList<String>) saleOrderService.uploadFile(rows);
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
	

	@RequestMapping(value = "/searchSaleOrderDetail", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSoDetail> searchSaleOrderDetail(@RequestBody int saleOrderesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderes()");
		List<SlsTblSoDetail> saleOrders = saleOrderService.searchSaleOrderDetail(saleOrderesId);
	
		return saleOrders;
  }
	
	@RequestMapping(value = "/getloginCustomer", method = RequestMethod.GET)
	public CfgTblUser getloginCustomer(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getloginCustomer()");

		
		return this.loginDao.getUserInformation(commonService.getCurrentLoggedInUser());

	}
	
	
	
	@RequestMapping(value = "/searchSaleOrderDetailSchedule", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleItemSchedule> searchSaleOrderDetailSchedule(@RequestBody int saleOrderesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchSaleOrderDetailSchedule()");
		List<SlsTblSaleItemSchedule> saleOrders = saleOrderService.searchSaleOrderDetailSchedule(saleOrderesId,false);
	
		return saleOrders;
  }
	
	@RequestMapping(value = "/searchSaleOrderDetailScheduleforDCForm", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleItemSchedule> searchSaleOrderDetailScheduleforDCForm(@RequestBody int saleOrderesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchSaleOrderDetailSchedule()");
		List<SlsTblSaleItemSchedule> saleOrders = saleOrderService.searchSaleOrderDetailSchedule(saleOrderesId,true);
	
		return saleOrders;
  }
	
	
	@RequestMapping(value="/getSOPicture",method=RequestMethod.GET ,produces = MediaType.IMAGE_JPEG_VALUE)
	public @ResponseBody byte[] getProductPicture(@RequestParam("id") String id, HttpServletRequest request, HttpServletResponse response){
		
		try {
			byte[] picture=  saleOrderService.getSOPicture(id);
			return picture;
    		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	@RequestMapping(value = "/updateSaleOrderfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSaleOrderfromSAP(@RequestBody SlsTblSaleOrder slsTblSaleOrder,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			
			if(slsTblSaleOrder.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblSaleOrder.getTxtSapNo());
				System.out.println(slsTblSaleOrder.getTxtStatus());
				System.out.println(slsTblSaleOrder.getTxtInvoiceStatus());
				System.out.println(slsTblSaleOrder.getTxtDCStatus());
				System.out.println(slsTblSaleOrder.getDteFinalApproval());
				System.out.println(slsTblSaleOrder.getDteRSMApproval());
				
				return saleOrderService.updateSaleOrderfromSAP(slsTblSaleOrder);
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
	
	
	@RequestMapping(value = "/updateSaleOrderPricefromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSaleOrderPricefromSAP(@RequestBody SlsTblSaleOrder slsTblSaleOrder,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			
			if(slsTblSaleOrder.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblSaleOrder.getTxtSapNo());
				System.out.println(slsTblSaleOrder.getTxtStatus());
				System.out.println(slsTblSaleOrder.getTxtInvoiceStatus());
				System.out.println(slsTblSaleOrder.getTxtDCStatus());
				System.out.println(slsTblSaleOrder.getDteFinalApproval());
				System.out.println(slsTblSaleOrder.getDteRSMApproval());
				
				return saleOrderService.updateSaleOrderfromSAP(slsTblSaleOrder);
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
	
	
	@RequestMapping(value = "/AddSaleOrderfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String AddSaleOrderfromSAP(@RequestBody SlsTblSaleOrder slsTblSaleOrder,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("addNewBooking()");
		
		try {
			
			if(slsTblSaleOrder.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblSaleOrder.getTxtSapNo());
				System.out.println(slsTblSaleOrder.getTxtStatus());
				System.out.println(slsTblSaleOrder.getTxtInvoiceStatus());
				System.out.println(slsTblSaleOrder.getTxtDCStatus());
				System.out.println(slsTblSaleOrder.getDteFinalApproval());
				System.out.println(slsTblSaleOrder.getDteRSMApproval());
				
				return saleOrderService.updateSaleOrderfromSAP(slsTblSaleOrder);
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
	

	@RequestMapping(value="/DC",method=RequestMethod.GET)
	public void getDCReport(String dc,HttpServletRequest request, HttpServletResponse response){
		try{
			
			
            JAXBContext context = JAXBContext.newInstance();

			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			StringWriter sw = new StringWriter();
			String header_par = "";
    		header_par = sw.toString();
			String request1  = ServerConfiguration.ip_servre+"/zsd_do_sf?sap-client=800&DO_NUM="+dc;
			URL url = new URL(request1);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setUseCaches(false);
			StringBuffer ab = new StringBuffer();
			conn.setDoOutput(true);
			Reader in;
			try {
				
				    InputStream inout = new BufferedInputStream(url.openStream());
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                OutputStream outStream = response.getOutputStream();
	                response.setHeader("Content-Disposition","filename=DC("+dc+").pdf");
		            response.setContentType("application/pdf");
	                byte[] buf = new byte[131072];
	                int n = 0;
	                while (-1 != (n = inout.read(buf))) {
	                	outStream.write(buf, 0, n);
	                }
            
			} catch (Exception e) {
				e.printStackTrace();
				ab.append(conn.getResponseMessage());
				conn.getErrorStream();
				// TODO: handle exception
			}
			conn.disconnect();
			


		} catch (PropertyException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
//			return "";

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			return "";
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			return "";
		}
	           
	        
	     
			catch (Exception e)
			{
				e.printStackTrace();
			}
       
		
	}
	
	
	
	@RequestMapping(value="/Invoice",method=RequestMethod.GET)
	public void getInvoiceReport(String invoice,HttpServletRequest request, HttpServletResponse response){
		try{
			
			
            JAXBContext context = JAXBContext.newInstance();

			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			StringWriter sw = new StringWriter();
			String header_par = "";
    		header_par = sw.toString();
			String request1  = ServerConfiguration.ip_servre+"/zsd_invc_sf?sap-client=800&INVC_NUM="+invoice;
			URL url = new URL(request1);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setUseCaches(false);
			StringBuffer ab = new StringBuffer();
			conn.setDoOutput(true);
			Reader in;
			try {
				
				    InputStream inout = new BufferedInputStream(url.openStream());
	                ByteArrayOutputStream out = new ByteArrayOutputStream();
	                OutputStream outStream = response.getOutputStream();
	                response.setHeader("Content-Disposition","filename=Invoice("+invoice+").pdf");
		            response.setContentType("application/pdf");
	                byte[] buf = new byte[131072];
	                int n = 0;
	                while (-1 != (n = inout.read(buf))) {
	                	outStream.write(buf, 0, n);
	                }
            
			} catch (Exception e) {
				e.printStackTrace();
				ab.append(conn.getResponseMessage());
				conn.getErrorStream();
				// TODO: handle exception
			}
			conn.disconnect();
			


		} catch (PropertyException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
//			return "";

		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			return "";
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			return "";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			return "";
		}
	           
	        
	     
			catch (Exception e)
			{
				e.printStackTrace();
			}
       
		
	}

	@RequestMapping(value = "/ApproveSaleOrder", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveSaleOrder(@RequestBody SlsTblSaleOrder dto, HttpServletRequest request,
												  HttpServletResponse response) {
		try {
			String status = "";
			List<String> idList = new ArrayList<String>();
			SlsTblSaleOrder s1;
			for (String id : dto.getTxtSaleOrderNo().split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);

			}
			status =  saleOrderService.updateSaleOrder(idList,null);// Email Functionality
			/*EmailService emailService = new EmailService();
			CfgTblUser user = userService.getAdminOfCurrentUserRole(commonService.getCurrentLoggedInUser());
			List<String> recipients = Arrays.asList(user.getTxtAddress());

			if
			emailService.sendEmail(
					recipients,
					"Invoice Approved - Procurement",
					"Your invoice " + dto.getTxtInvoiceNo() +" has been approved."
			);*/
			SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
			if (dto.getTxtSaleOrderNo() != null && !dto.getTxtSaleOrderNo().isEmpty()) {
				slsTblSaleOrder.setSerSaleOrderId(Integer.parseInt(dto.getTxtSaleOrderNo()));
			}

			List<SlsTblSaleOrder> slsTblSaleOrders = saleOrderService.searchSaleOrder(slsTblSaleOrder);
			List<CfgTblCustomer> lstDealer;
			List<String> addressesVendor = null;

			// Process Sale Orders
			if (slsTblSaleOrders != null && !slsTblSaleOrders.isEmpty()) {
				SlsTblSaleOrder firstSaleOrder = slsTblSaleOrders.get(0);
				SlsTblDeal firstDeal = firstSaleOrder.getSlsTblDeal();

				if (firstDeal != null && firstDeal.getCfgTblDealer() != null) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(firstDeal.getCfgTblDealer().getTxtCustomerCode());
					lstDealer = customerService.searchCustomer(cust);
				} else {
					lstDealer = null;
					System.out.println("No deal found for the first sale order.");
				}
			} else {
				lstDealer = null;
				System.out.println("No sale orders found.");
			}

			// Fetch Vendor Addresses
			if (lstDealer != null && !lstDealer.isEmpty()) {
				List<CfgTblUser> users = citTableUserDAO.getActiveUser();
				if (users != null) {
					addressesVendor = users.stream()
							.filter(user -> user.getCfgTblCustomer() != null
									&& lstDealer.get(0).getSerCustomerId().equals(user.getCfgTblCustomer().getSerCustomerId()))
							.map(CfgTblUser::getTxtAddress)  // Extract txtAddress
							.filter(txtAddress -> txtAddress != null && !txtAddress.isEmpty())
							.collect(Collectors.toList());
				}
			} else {
				System.out.println("No customers found.");
			}

			// Send Email Notifications
			EmailService emailService = new EmailService();
			CfgTblUser user = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
			List<String> recipients = new ArrayList<>();

			if (user != null) {
				recipients = commonService.getAddressesBasedOnRoleHierarchy(user);
			}

			// Add vendor addresses if available
			if (addressesVendor != null && !addressesVendor.isEmpty()) {
				recipients.addAll(addressesVendor);
			}

			// Send email
			emailService.sendEmail(
					recipients,
					"Invoice Approved",
					"Your invoice " + dto.getTxtSaleOrderNo() + " has been approved."
			);

			if (addressesVendor != null && !addressesVendor.isEmpty()) {
				emailService.sendEmail(
						addressesVendor,
						"Invoice Updated",
						"Your invoice " + dto.getTxtSaleOrderNo() + " has been updated."
				);
			}
			return status;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}
	/*@RequestMapping(value = "/ApproveSaleOrder", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveSaleOrder(@RequestBody String saleOrderesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			SlsTblSaleOrder s1;
			for (String id : saleOrderesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			
			}
			return saleOrderService.updateSaleOrder(idList,null);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}*/
	
	
	
	@RequestMapping(value = "/ApproveSaleOrderinListWithDate", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveSaleOrder(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			SlsTblSaleOrder s1;
			for (String id : dto.getTxtDivision().split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			
			}
			return saleOrderService.updateSaleOrder(idList,dto.getDteDate());
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateSaleOrderValuefromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSaleOrderValuefromSAP(@RequestBody SlsTblSaleOrder slsTblSaleOrder,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("updateSaleOrderValuefromSAP()");
		
		try {
			
			if(slsTblSaleOrder.getTxtSapNo().trim().length()>0)
			{
				System.out.println(slsTblSaleOrder.getTxtSapNo());
				System.out.println(slsTblSaleOrder.getTxtStatus());
				System.out.println(slsTblSaleOrder.getTxtInvoiceStatus());
				System.out.println(slsTblSaleOrder.getTxtDCStatus());
				System.out.println(slsTblSaleOrder.getDteFinalApproval());
				System.out.println(slsTblSaleOrder.getDteRSMApproval());
				slsTblSaleOrder.setTxtMachineIp("netAmount");
				return saleOrderService.updateSaleOrderfromSAP(slsTblSaleOrder);
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
	
	
	
	@RequestMapping(value = "/AddNewSaleOrderfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String AddNewSaleOrderfromSAP(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		logger.debug("addNewBooking()");

		try {

			if (dto.getTxtOrderNO().trim().length() > 0) {

				SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
				slsTblSaleOrder.setBlnFromSAP(true);
				slsTblSaleOrder.setDteDate(dto.getDteDate());
				slsTblSaleOrder.setTxtSapNo(dto.getTxtOrderNO());
				slsTblSaleOrder.setTxtSaleOrderNo(dto.getTxtOrderNO());
				slsTblSaleOrder.setTxtPONo(dto.getTxtPurchaseNo());

				if (dto.getTxtDealer() != null && dto.getTxtDealer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtDealer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblSaleOrder.setCfgTblDealer(lstDealer.get(0));
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
						slsTblSaleOrder.setCfgTblDealer(cust);
					}
				}

				if (dto.getTxtCustomer() != null && dto.getTxtCustomer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtCustomer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblSaleOrder.setCfgTblCustomer(lstDealer.get(0));
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
						slsTblSaleOrder.setCfgTblCustomer(cust);
					}
				}

				if (dto.getTxtItemNo() != null && dto.getTxtItemNo().trim().length() > 0) {
					CfgTblProduct prd = new CfgTblProduct();
					prd.setTxtProductCode(dto.getTxtItemNo());
					List<CfgTblProduct> lstProduct = productService.searchProduct(prd);
					List lstDetail = new ArrayList();
					if (lstProduct != null && lstProduct.size() > 0) {
						slsTblSaleOrder.setCfgTblProduct(lstProduct.get(0));
						slsTblSaleOrder.setNumQuantity(dto.getNumQty());

						SlsTblSoDetail detail = new SlsTblSoDetail();
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
						slsTblSaleOrder.setSlsTblSoDetails(lstDetail);
					}

				}

				return saleOrderService.addNewSaleOrder(slsTblSaleOrder);

			} else
				return "01-Sap No. is missing";

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	
	@RequestMapping(value = "/UpdateNewSaleOrderfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String UpdateNewSaleOrderfromSAP(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		logger.debug("addNewBooking()");

		try {
			
			SlsTblSaleOrder slsOrder=new SlsTblSaleOrder();
			if (dto.getTxtOrderNO().trim().length() > 0) {
				slsOrder.setTxtSapNo(dto.getTxtOrderNO());
				List<SlsTblSaleOrder> lstOrder = saleOrderService.searchSaleOrder(slsOrder);
				SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
				if(lstOrder!=null && lstOrder.size() >0)
				{
					slsTblSaleOrder=lstOrder.get(0);
				}
				else return "Order Not found";
				
				slsTblSaleOrder.setBlnFromSAP(true);
				slsTblSaleOrder.setDteDate(dto.getDteDate());
				slsTblSaleOrder.setTxtSapNo(dto.getTxtOrderNO());
				slsTblSaleOrder.setTxtSaleOrderNo(dto.getTxtOrderNO());
				slsTblSaleOrder.setTxtPONo(dto.getTxtPurchaseNo());

				if (dto.getTxtDealer() != null && dto.getTxtDealer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtDealer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblSaleOrder.setCfgTblDealer(lstDealer.get(0));
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
						slsTblSaleOrder.setCfgTblDealer(cust);
					}
				}

				if (dto.getTxtCustomer() != null && dto.getTxtCustomer().trim().length() > 0) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(dto.getTxtCustomer());
					List<CfgTblCustomer> lstDealer = customerService.searchCustomer(cust);
					if (lstDealer != null && lstDealer.size() > 0) {
						slsTblSaleOrder.setCfgTblCustomer(lstDealer.get(0));
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
						slsTblSaleOrder.setCfgTblCustomer(cust);
					}
				}

				if (dto.getTxtItemNo() != null && dto.getTxtItemNo().trim().length() > 0) {
					CfgTblProduct prd = new CfgTblProduct();
					prd.setTxtProductCode(dto.getTxtItemNo());
					List<CfgTblProduct> lstProduct = productService.searchProduct(prd);
					List lstDetail = new ArrayList();
					if (lstProduct != null && lstProduct.size() > 0) {
						SlsTblSoDetail detail = new SlsTblSoDetail();
						List<SlsTblSoDetail> saleOrders = saleOrderService.searchSaleOrderDetail(slsTblSaleOrder.getSerSaleOrderId());
						if(saleOrders !=null && saleOrders.size() >0)
							detail=saleOrders.get(0);
						
						slsTblSaleOrder.setCfgTblProduct(lstProduct.get(0));
						slsTblSaleOrder.setNumQuantity(dto.getNumQty());

						
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
						slsTblSaleOrder.setSlsTblSoDetails(lstDetail);
					}

				}

				return saleOrderService.updateSaleOrder(slsTblSaleOrder);

			} else
				return "01-Sap No. is missing";

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	@RequestMapping(value = "/addNewSaleOrderPayment", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewSaleOrderPayment(@RequestBody SlsTblSoPayments slsTblSoPayments, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			
			
			List<SlsTblSoPayments> lstPayment= salesDAO.CheckPaymentDuplicationChequeNo(slsTblSoPayments);
			
			List<SlsTblSoPayments> lstPaymentREC= salesDAO.CheckPaymentDuplicationtxtSlipNo(slsTblSoPayments);
			
			if(lstPayment!=null && lstPayment.size() >0)
			{
				if(lstPaymentREC!=null && lstPaymentREC.size() >0)
				{
					return "DCH&SLIP";
				}
				else
					return "DCH";
			}
			else if(lstPaymentREC!=null && lstPaymentREC.size() >0)
			{
				return "DSLIP";
			}
	
			return saleOrderService.addNewSaleOrderPayment(slsTblSoPayments);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/getAllSaleOrderPayments", method = RequestMethod.GET)
	public List<SlsTblSoPayments> getAllSaleOrderPayments(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderPayments()");
		List<SlsTblSoPayments> saleOrders = saleOrderService.getAllSaleOrderPayments();
		return saleOrders;
	}
	
	
	@RequestMapping(value="/searchSaleOrderPayments",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSoPayments> searchSaleOrderPayments(@RequestBody SlsTblSoPayments slsTblSaleOrder,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchSaleOrderPayments()");
		List<SlsTblSoPayments> saleOrders = saleOrderService.searchSaleOrderPayments(slsTblSaleOrder);
		return saleOrders;
		
	}
	
	
	@RequestMapping(value="/searchSaleOrderPaymentsNew",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSoPayments> searchSaleOrderPaymentsNew(@RequestBody ReportDTO dto,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchSaleOrderPayments()");
		List<SlsTblSoPayments> saleOrders = salesDAO.searchSaleOrderPayments(dto);
		return saleOrders;
		
	}
	
	@RequestMapping(value="/uploadCandidateDocument",method=RequestMethod.POST)
	public String uploadCandidateDocument(@RequestParam(value = "newDocument") String candidateDocumentInfo,
					@RequestParam(value = "file") MultipartFile file, HttpServletRequest request, HttpServletResponse response){
		ObjectMapper mapper = new ObjectMapper();
		try {
			SOPaymentDocument paymentDocument =mapper.readValue(candidateDocumentInfo,SOPaymentDocument.class);
			paymentDocument.setDocumentFile(file.getBytes());
			paymentDocument.setDocumentName(paymentDocument.getDocumentName());
			paymentDocument.setDocumentType(file.getContentType());
			paymentDocument.setOriginalName(file.getOriginalFilename());
			paymentDocument.setSize(file.getSize()+"");
			saleOrderService.uploadPaymentDocument(paymentDocument);
			return "{\"status\":\"Success\"}";
		}catch(Exception ex){
			System.out.println("parsing issue" + ex.getMessage());
			
		}

		return "{\"status\":\"Failure\"}";
	}
	
	
	@RequestMapping(value="/downloadDocument",method= RequestMethod.POST)
	public void downloadDocument(@RequestBody String documentId , HttpServletRequest request, HttpServletResponse response){
		 
		byte[] pdf= saleOrderService.downloadDocument(Integer.parseInt(documentId));
		 response.setContentType("application/x-download");
		    response.setHeader("Content-Disposition", "attachment; filename=foo.pdf");
		    response.setHeader("Pragma", "no-cache");
		    response.setHeader("Cache-Control", "no-cache");
		   
		    try {
				response.getOutputStream().write(pdf);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	@RequestMapping(value = "/removeCandidateDocument", method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String removeCandidateDocument(@RequestBody String documentId, HttpServletRequest request, HttpServletResponse response){
		try{
			return saleOrderService.removeCandidateDocument(Integer.parseInt(documentId));
		}catch(Exception ex){
//			log.error("Unable to Remove Candidate Document"+ex.getMessage(),ex);
		}
		
		return "Failure";
	}
	
	@RequestMapping(value = "/getCandidateDocument", method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SOPaymentDocument> getCandidateDocument(@RequestBody String candidateId, HttpServletRequest request, HttpServletResponse response){
		try{
			return saleOrderService.getSOPaymentDocumentList(Integer.parseInt(candidateId));
//			return setupServiceDao.getCustomerContracts(candidateId);
		}catch(Exception ex){
//			log.error("Unable to Remove Candidate Document"+ex.getMessage(),ex);
		}
		
		return null;
	}
	
	
	/*@RequestMapping(value = "/ApproveSaleOrderinListWithDateandLevel", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveSaleOrderinListWithDate2(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			String status = "";
			List<String> idList = new ArrayList<String>();
			SlsTblSaleOrder s1;
			for (String id : dto.getTxtDivision().split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			
			}
			
			if(dto.getLevel()==0)
				dto.setLevel(1);
			status = saleOrderService.updateSaleOrder(idList,dto.getDteDate(),dto.getLevel(),dto);// Email Functionality
			SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
			slsTblSaleOrder.setSerSaleOrderId(Integer.parseInt(dto.getTxtDivision()));
			List<CfgTblCustomer> lstDealer;
			List<String> addressesVendor = null;
			List<SlsTblSaleOrder> slsTblSaleOrders = saleOrderService.searchSaleOrder(slsTblSaleOrder);
			if (slsTblSaleOrders != null && !slsTblSaleOrders.isEmpty()) {
				SlsTblSaleOrder firstSaleOrder = slsTblSaleOrders.get(0);
				SlsTblDeal firstDeal = firstSaleOrder.getSlsTblDeal();
				if (firstDeal != null) {
					System.out.println("Deal ID of first sale order: " + firstDeal.getSerDealId());
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(firstDeal.getTxtDealer());
					 lstDealer = customerService.searchCustomer(cust);
					*//*if (lstDealer != null && lstDealer.size() > 0) {
						slsTblDeal.setCfgTblDealer(lstDealer.get(0));
					}*//*
				} else {
					lstDealer = null;
				}
			} else {
				lstDealer = null;
				System.out.println("No sale orders found.");
			}
			if (lstDealer != null && !lstDealer.isEmpty()) {


				List<CfgTblUser> users = citTableUserDAO.getActiveUser();

				 addressesVendor = users.stream()
						.filter(user -> user.getCfgTblCustomer() != null && lstDealer.get(0).getSerCustomerId().equals(user.getCfgTblCustomer().getSerCustomerId()))
						.map(CfgTblUser::getTxtAddress)  // Extract txtAddress
						.filter(txtAddress -> txtAddress != null && !txtAddress.isEmpty())
						.collect(Collectors.toList());
			} else {
				System.out.println("No customers found.");
			}
			EmailService emailService = new EmailService();
			CfgTblUser user = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
			List<String> recipients = new ArrayList<>();
			recipients = commonService.getAddressesBasedOnRoleHierarchy(user);
			*//*recipients = (user.getTxtAddress() != null && !user.getTxtAddress().isEmpty())
					? Arrays.asList(user.getTxtAddress())
					: (List<String>) userService.getAdminOfCurrentUserRole(commonService.getCurrentLoggedInUser());*//*
			recipients.add(addressesVendor.toString());

			emailService.sendEmail(
					recipients,
					"Invoice Approved",
					"Your invoice " + dto.getTxtDivision() + " has been approved."
			);

			emailService.sendEmail(
					Collections.singletonList(addressesVendor.toString()),
					"Invoice Approved",
					"Your invoice " + dto.getTxtDivision() + " has been updated."
			);

			return status;
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}*/

	@RequestMapping(value = "/ApproveSaleOrderinListWithDateandLevel", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveSaleOrderinListWithDate2(@RequestBody SODTO dto, HttpServletRequest request,
												  HttpServletResponse response) {
		try {
			String status = "";
			List<String> idList = new ArrayList<>();

			// Parse txtDivision and handle empty or null values
			if (dto.getTxtDivision() != null && !dto.getTxtDivision().isEmpty()) {
				for (String id : dto.getTxtDivision().split(",")) {
					if (!id.isEmpty()) {
						idList.add(id);
					}
				}
			}

			// Default level to 1 if not provided
			if (dto.getLevel() == 0) {
				dto.setLevel(1);
			}

			// Update Sale Order
			status = saleOrderService.updateSaleOrder(idList, dto.getDteDate(), dto.getLevel(), dto);

			// Initialize and search Sale Order
			SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
			if (dto.getTxtDivision() != null && !dto.getTxtDivision().isEmpty()) {
				slsTblSaleOrder.setSerSaleOrderId(Integer.parseInt(dto.getTxtDivision()));
			}

			List<SlsTblSaleOrder> slsTblSaleOrders = saleOrderService.searchSaleOrder(slsTblSaleOrder);
			List<CfgTblCustomer> lstDealer;
			List<String> addressesVendor = null;

			// Process Sale Orders
			if (slsTblSaleOrders != null && !slsTblSaleOrders.isEmpty()) {
				SlsTblSaleOrder firstSaleOrder = slsTblSaleOrders.get(0);
				SlsTblDeal firstDeal = firstSaleOrder.getSlsTblDeal();

				if (firstDeal != null && firstDeal.getCfgTblDealer() != null) {
					CfgTblCustomer cust = new CfgTblCustomer();
					cust.setTxtCustomerCode(firstDeal.getCfgTblDealer().getTxtCustomerCode());
					lstDealer = customerService.searchCustomer(cust);
				} else {
					lstDealer = null;
					System.out.println("No deal found for the first sale order.");
				}
			} else {
				lstDealer = null;
				System.out.println("No sale orders found.");
			}

			// Fetch Vendor Addresses
			if (lstDealer != null && !lstDealer.isEmpty()) {
				List<CfgTblUser> users = citTableUserDAO.getActiveUser();
				if (users != null) {
					addressesVendor = users.stream()
							.filter(user -> user.getCfgTblCustomer() != null
									&& lstDealer.get(0).getSerCustomerId().equals(user.getCfgTblCustomer().getSerCustomerId()))
							.map(CfgTblUser::getTxtAddress)  // Extract txtAddress
							.filter(txtAddress -> txtAddress != null && !txtAddress.isEmpty())
							.collect(Collectors.toList());
				}
			} else {
				System.out.println("No customers found.");
			}

			// Send Email Notifications
			EmailService emailService = new EmailService();
			CfgTblUser user = commonService.getCurrentUser(commonService.getCurrentLoggedInUser());
			List<String> recipients = new ArrayList<>();

			if (user != null) {
				recipients = commonService.getAddressesBasedOnRoleHierarchy(user);
			}

			// Add vendor addresses if available
			if (addressesVendor != null && !addressesVendor.isEmpty()) {
				recipients.addAll(addressesVendor);
			}

			// Send email
			emailService.sendEmail(
					recipients,
					"Invoice Approved",
					"Your invoice " + dto.getTxtDivision() + " has been approved."
			);

			if (addressesVendor != null && !addressesVendor.isEmpty()) {
				emailService.sendEmail(
						addressesVendor,
						"Invoice Updated",
						"Your invoice " + dto.getTxtDivision() + " has been updated."
				);
			}

			if(status == "Success"){
				return "{\"status\":\"Success\"}";
			}else{
				return "{\"status\":\"Failure\"}";
			}

		} catch (Exception ex) {
			logger.error("Error in ApproveSaleOrderinListWithDateandLevel: ", ex);
			 return "{\"status\":\"Failure\"}";
		}
	}



	@RequestMapping(value = "/updateDeliveryOrdeStatusfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateDeliveryOrdeStatusfromSAP(@RequestBody SODTO dto,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("updateDeliveryOrdeStatusfromSAP()-------------------------------");
		logger.info("*****************************************************************");
		logger.info("pso----" + dto.getTxtPsoNo()+"flag---" + dto.isBlnFlag());
		logger.info("pso----" + dto.toString());
		logger.info("*****************************************************************");
		
		try {
			
			if(dto.getTxtPsoNo().trim().length()>0)
			{
				System.out.println(dto.getTxtPsoNo());
				System.out.println(dto.isBlnFlag());
				
				SlsTblSaleOrder slsTblSaleOrder =new SlsTblSaleOrder();
				slsTblSaleOrder.setTxtSaleOrderNo(dto.getTxtPsoNo());
				
				if(dto.getTxtDCNo()!=null && dto.getTxtDCNo().trim().length() > 0)
					slsTblSaleOrder.setTxtDCNo(dto.getTxtDCNo());
				
				if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length() > 0)
					slsTblSaleOrder.setTxtInvoiceNo(dto.getTxtInvoiceNo());
				
				
				if(dto.getTxtChassisNo()!=null && dto.getTxtChassisNo().trim().length() > 0)
				slsTblSaleOrder.setTxtChassisNo(dto.getTxtChassisNo());
				
				if(dto.getTxtEngineNo()!=null && dto.getTxtEngineNo().trim().length() > 0)
				slsTblSaleOrder.setTxtEngineNo(dto.getTxtEngineNo());
				
				if(dto.getTxtRegistrationNo()!=null && dto.getTxtRegistrationNo().trim().length() > 0)
				slsTblSaleOrder.setTxtRegistrationNo(dto.getTxtRegistrationNo());
				
				if(dto.getTxtDeliveryPartNo()!=null && dto.getTxtDeliveryPartNo().trim().length() > 0)
					slsTblSaleOrder.setTxtDeliveryPartNo(dto.getTxtDeliveryPartNo());
				
				if(dto.getDteDeliveryDate()!=null )
					slsTblSaleOrder.setDteDeliveryDateActual(dto.getDteDeliveryDate());
				
				if(dto.getNumQty()!=null )
					slsTblSaleOrder.setNumQtyActual(dto.getNumQty());
				
				if(dto.getLstDetail()!=null && dto.getLstDetail().size()>0)
				{
					List<SlsTblSoDetail> sodetails=new ArrayList();
					for(SODetailDTO dtoDetail : dto.getLstDetail() )
					{
						if(dtoDetail!=null)
						{
							CfgTblProduct product=new CfgTblProduct();
							product.setTxtProductCode(dtoDetail.getItemNo());
							
							List<CfgTblProduct> products = productService.searchProduct(product);
							if(products!= null && products.size() >0)
							{
								CfgTblProduct prod=products.get(0);
								SlsTblSoDetail soDetail=new SlsTblSoDetail();
								soDetail.setCfgTblProduct(prod);
								if(dtoDetail.getNumQty()!=null && dtoDetail.getNumQty().doubleValue() > 0)
								soDetail.setNumIssueQty(dtoDetail.getNumQty());
								sodetails.add(soDetail);
							}
							else
							{
								return "Item No. ("+dtoDetail.getItemNo()+") not Found.";
							}
						}
					}
					
					slsTblSaleOrder.setSlsTblSoDetails(sodetails);
				}
				
							
				
				
				
				if(dto.isBlnFlag())
				{
				slsTblSaleOrder.setTxtDCStatus("Approved");
				return saleOrderService.updateSaleOrderfromSAP(slsTblSaleOrder);
				}
				else
				{
					return "Failure";
				}
			}
			else
				return "01- PsoNo is missing";
			
			

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	@RequestMapping(value = "/updateInvoiceStatusfromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateInvoiceStatusfromSAP(@RequestBody SODTO dto,
			HttpServletRequest request, HttpServletResponse response) {
		logger.debug("updateInvoiceStatusfromSAP()----------------------------------");
		logger.info("*****************************************************************");
		logger.info("method----" + dto.toString());
		logger.info("*****************************************************************");
		try {
			
			if(dto.getTxtPsoNo().trim().length()>0)
			{
				System.out.println(dto.getTxtPsoNo());
				System.out.println(dto.isBlnFlag());
				
				SlsTblSaleOrder slsTblSaleOrder =new SlsTblSaleOrder();
				slsTblSaleOrder.setTxtSaleOrderNo(dto.getTxtPsoNo());
				if(dto.getTxtDCNo()!=null && dto.getTxtDCNo().trim().length() > 0)
					slsTblSaleOrder.setTxtDCNo(dto.getTxtDCNo());
				
				if(dto.getTxtInvoiceNo()!=null && dto.getTxtInvoiceNo().trim().length() > 0)
					slsTblSaleOrder.setTxtInvoiceNo(dto.getTxtInvoiceNo());
				
				
				if(dto.isBlnFlag())
				{
				slsTblSaleOrder.setTxtInvoiceStatus("Approved");
				return saleOrderService.updateSaleOrderfromSAP(slsTblSaleOrder);
				}
				else
				{
					return "Failure";
				}
			}
			else
				return "01- PsoNo is missing";
			
			

//			return "{\"status\":\"Failure\"}";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}


	@RequestMapping(value = "/deletePayment", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deletePayment(@RequestBody String PaymentId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : PaymentId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return saleOrderService.deletePayment(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}
	
	
	@RequestMapping(value = "/SentToostoSaleOrder", method = RequestMethod.POST)
	public String SentToostoSaleOrder(@RequestParam(value = "so", required = false) String soString,@RequestParam(value = "soDetails", required = false) String soDetailsString
			, HttpServletRequest request, Model model)
			throws IllegalStateException, IOException {
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
			System.out.println("soDetailsString is------------" + soDetailsString);
		

			SlsTblSaleOrder slsTblSaleOrder = mapper.readValue(soString, SlsTblSaleOrder.class);
			
			saleOrderService.updateSaleOrder(slsTblSaleOrder);
			
			CfgTblProduct[]  lsttoolDetail = null;
				if(soDetailsString!=null && soDetailsString.length() > 2)
					lsttoolDetail=mapper.readValue(soDetailsString, CfgTblProduct[].class);
			List<CfgTblProduct> listTools = new ArrayList();
			if(lsttoolDetail!= null)
				listTools=Arrays.asList(lsttoolDetail);
			
			List<SlsTblToolDetail> lstToolDetail=new ArrayList(); 
			SlsTblToolDetail obj;
			for(int i=0; i < listTools.size() ; i++)
			{
				obj=new SlsTblToolDetail();
				obj.setSlsTblSaleOrder(slsTblSaleOrder);
				obj.setCfgTblProduct(listTools.get(i));
				obj.setNumQuantity(listTools.get(i).getNumOldPrice());
				lstToolDetail.add(obj);
			}

			return saleOrderService.addToolsDetail(lstToolDetail);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	@RequestMapping(value = "/searchSaleOrderToolDetail", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblToolDetail> searchSaleOrderToolDetail(@RequestBody int saleOrderesId,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("searchSaleOrderToolDetail()");
		List<SlsTblToolDetail> saleOrders = saleOrderService.searchSaleOrderToolDetail(saleOrderesId);
	
		return saleOrders;
  }
	
	
	@RequestMapping(value = "/UpdateToostoSaleOrder", method = RequestMethod.POST)
	public String UpdateToostoSaleOrder(@RequestParam(value = "so", required = false) String soString,@RequestParam(value = "soDetails", required = false) String soDetailsString
			, HttpServletRequest request, Model model)
			throws IllegalStateException, IOException {
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			
			System.out.println("soDetailsString is------------" + soDetailsString);
		

			SlsTblSaleOrder slsTblSaleOrder = mapper.readValue(soString, SlsTblSaleOrder.class);
			
			SlsTblToolDetail[]  lsttoolDetail = null;
				if(soDetailsString!=null && soDetailsString.length() > 2)
					lsttoolDetail=mapper.readValue(soDetailsString, SlsTblToolDetail[].class);
			List<SlsTblToolDetail> listTools = new ArrayList();
			if(lsttoolDetail!= null)
				listTools=Arrays.asList(lsttoolDetail);
			
		
		
			


			return saleOrderService.updateToolsDetail(listTools);
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}
	
	
	
	
	@RequestMapping(value = "/cancelSaleOrderFromSAP", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String cancelSaleOrderFromSAP(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		
		logger.debug("SO Cancel---()----------------------------------");
		logger.info("*****************************************************************");
		logger.info("method----" + dto.toString());
		logger.info("*****************************************************************");
		
		
		
		String Order_No="";
		SlsTblSaleOrder slsOrder=new SlsTblSaleOrder();
		if (dto.getTxtPsoNo().trim().length() > 0) {
			slsOrder.setTxtSaleOrderNo(dto.getTxtPsoNo());
			slsOrder.setTxtMachineIp("22");
			List<SlsTblSaleOrder> lstOrder = saleOrderService.searchSaleOrder(slsOrder);
			SlsTblSaleOrder slsTblSaleOrder = new SlsTblSaleOrder();
			if(lstOrder!=null && lstOrder.size() >0)
			{
				slsTblSaleOrder=lstOrder.get(0);
				Order_No=slsTblSaleOrder.getSerSaleOrderId().toString();
			}
		
			else return "Sale Order Not found";
		}
		else return "Kindly Provide PSO No.";
			
		try {
			
			
			
				
				
			List<String> idList = new ArrayList<String>();
//			for (String id : saleOrderesId.split(",")) {
//				if (id.isEmpty()) {
//					continue;
//				}
//				idList.add(id);
//			}
			
			idList.add(Order_No);
			return salesDAO.deleteSaleOrder(idList,dto.getTxtReason());
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}
	
	
	
	@RequestMapping(value="/updateSaleOrderSAP",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateSaleOrderSAP(@RequestBody SlsTblSaleOrder slsTblSaleOrder,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("updateSaleOrderSAP()");
		return salesDAO.updateSaleOrder(slsTblSaleOrder);
	
		
	}
	
	
	
	@RequestMapping(value = "/ApproveSaleOrderPaymentsinListWithDateandLevel", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String ApproveSaleOrderPaymentsinListWithDateandLevel(@RequestBody SODTO dto, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			SlsTblSaleOrder s1;
			for (String id : dto.getTxtDivision().split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			
			}
			
			if(dto.getLevel()==0)
				dto.setLevel(1);
			return salesDAO.updateSaleOrderPayment(idList,dto.getLevel());
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}


	@RequestMapping(value = "/addNewSaleOrderNew", method = RequestMethod.POST)
	public String addNewSaleOrderNewAction(
			@RequestParam(value = "so", required = false) String soString,
			@RequestParam(value = "newDocuments", required = false) String documentInfoListJson,
			@RequestParam(value = "file", required = false) MultipartFile[] files,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			SlsTblSaleOrder slsTblSaleOrder = mapper.readValue(soString, SlsTblSaleOrder.class);
			String timeString = slsTblSaleOrder.getTimeString();
		/*	LocalTime time = LocalTime.parse(timeString);*/
			/*LocalDateTime localDateTime = LocalDateTime.parse(slsTblSaleOrder.getDteIssueTime().replace(" ", "T"));
			Timestamp timestamp = Timestamp.valueOf(localDateTime);*/
			slsTblSaleOrder.setTimeString(timeString);
			JsonNode rootNode = mapper.readTree(soString);
			if (rootNode.has("slsTblSoDetails")) {
				List<SlsTblSoDetail> detailsList = mapper.readValue(
						rootNode.get("slsTblSoDetails").toString(),
						new TypeReference<List<SlsTblSoDetail>>() {}
				);
				slsTblSaleOrder.setSlsTblSoDetails(detailsList);
			}
			List<SlsTblSoDetail> dealsDetailsList = slsTblSaleOrder.getSlsTblSoDetails();
			for (SlsTblSoDetail dealDetail : dealsDetailsList) {
				// Assuming dealDetail has a 'lineItemWithId' field in the format "item-name-12345"
				String lineItemWithId = dealDetail.getSapLineItem();

				// Split the lineItemWithId using the hyphen as a delimiter
				String[] parts = lineItemWithId.split("-");

				if (parts.length == 2) {
					String lineItem = parts[0];
					String serDealDetailId = parts[1];
					dealDetail.setSapLineItem(lineItem);
					SlsTblDealDetails slsTblDealDetails=new SlsTblDealDetails();
					slsTblDealDetails.setSerDealDetailId(Integer.valueOf(serDealDetailId));
					dealDetail.setSlsTblDealDetails(slsTblDealDetails);
					// Debugging or logging output
					System.out.println("Line Item: " + lineItem);
					System.out.println("Deal Detail ID: " + serDealDetailId);
				} else {
					// Handle the case where the format is not as expected
					System.out.println("Invalid format for lineItemWithId: " + lineItemWithId);
				}
			}
			List<SOPaymentDocument> paymentDocuments = new ArrayList<>();
			if (documentInfoListJson != null && !documentInfoListJson.isEmpty()) {
				paymentDocuments = mapper.readValue(documentInfoListJson, new TypeReference<List<SOPaymentDocument>>(){});
			}

			// Handle file upload if needed
			// Map the files to payment documents
			if (files != null && files.length > 0 && paymentDocuments.size() == files.length) {
				for (int i = 0; i < paymentDocuments.size(); i++) {
					SOPaymentDocument doc = paymentDocuments.get(i);
					MultipartFile file = files[i];
					doc.setDocumentFile(file.getBytes());
				}
			}
			/*if (files != null && !paymentDocuments.isEmpty()) {
				for (SOPaymentDocument doc : paymentDocuments) {
					for (MultipartFile file : files) {
						// Process each file as needed
						doc.setDocumentFile(file.getBytes());
					}

				}
			}*/

			// Call service method with list of documents
			 saleOrderService.addNewSaleOrderWithPaymentDocuments(slsTblSaleOrder, paymentDocuments);
			return "{\"status\":\"Success\"}";
		} catch (Exception ex) {
			ex.printStackTrace();
			return "{\"status\":\"Failure\"}";
		}
	}


	private boolean waitForFileToRelease(Path tempFilePath, int retries, long delay) {
		int attempt = 0;
		while (attempt < retries) {
			try {
				// Try to access the file by reading it
				Files.readAllBytes(tempFilePath);
				return true; // File is available
			} catch (IOException e) {
				attempt++;
				// File is locked, so retry after a delay
				try {
					Thread.sleep(delay);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt(); // Restore the interrupted status
				}
			}
		}
		return false; // If file couldn't be accessed after retries
	}

	// Forcefully delete a locked file
	private void deleteFileForcefully(Path filePath) {
		try {
			// Attempt to delete the file
			Files.deleteIfExists(filePath);
		} catch (IOException e) {
			System.err.println("Error forcefully deleting the file: " + filePath + " - " + e.getMessage());
		}
	}


	@RequestMapping(value = "/searchSaleOrderAudit", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<AuditSlsTblSaleOrder> searchSaleOrderAudit(@RequestBody int saleOrderesId,HttpServletRequest request, HttpServletResponse response) {
		List<AuditSlsTblSaleOrder> saleOrders = saleOrderService.searchSaleOrderAudit(saleOrderesId);
		return saleOrders;
	}


	@RequestMapping(value = "/repost", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String repostSES(@RequestBody ServiceDTO serviceDTO, HttpServletRequest request, HttpServletResponse response) throws Exception {
//		          return
 		          String status = salesDAO.updateSaleOrderPayment(serviceDTO);
				  if(status.equals("Success")){
					  return "{\"status\":\"Success\"}";
				  }else{
					  return "{\"status\":\"Failure\"}";
				  }
	 }


	@RequestMapping(value="/searchSaleOrderByDepartment",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SlsTblSaleOrder> searchSaleOrderByDepartmentAction(@RequestBody SlsTblSaleOrder slsTblSaleOrder,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderes()");
		List<SlsTblSaleOrder> saleOrders = saleOrderService.searchSaleOrderBtDepartment(slsTblSaleOrder);
		return saleOrders;

	}


	@RequestMapping(value="/uploadDocument",method=RequestMethod.POST)
	public String uploadDocument(@RequestParam("pdf") MultipartFile file,
								 @RequestParam("fileId") String fileId, HttpServletRequest request, HttpServletResponse response){
		ObjectMapper mapper = new ObjectMapper();
		try {

			EntityManager entityManager = getEntityManager();
			entityManager.getTransaction().begin();
			SOPaymentDocument paymentDocument = entityManager.find(SOPaymentDocument.class, Integer.valueOf(fileId));
			/*entityManager.close();*/
			/*SOPaymentDocument paymentDocument =mapper.readValue(candidateDocumentInfo,SOPaymentDocument.class);*/
			paymentDocument.setDocumentFile(file.getBytes());
			paymentDocument.setDocumentName(paymentDocument.getDocumentName());
			paymentDocument.setDocumentType(file.getContentType());
			paymentDocument.setOriginalName(file.getOriginalFilename());
			paymentDocument.setSize(file.getSize()+"");
			paymentDocument.setSlsTblSaleOrder(paymentDocument.getSlsTblSaleOrder());
			entityManager.persist(paymentDocument);
			entityManager.getTransaction().commit();
			entityManager.close();
			return "Success";
			/*return saleOrderService.uploadPaymentDocument(paymentDocument);*/
		}catch(Exception ex){
			System.out.println("parsing issue" + ex.getMessage());

		}

		return "Failure";
	}

	@Autowired
	private EntityManagerFactory entityManagerFactory;
	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}
}
