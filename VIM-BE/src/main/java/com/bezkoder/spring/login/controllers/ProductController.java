package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.dto.SPDTO;
import com.bezkoder.spring.login.sa.bll.services.*;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblCustomerDAO;
import com.bezkoder.spring.login.sa.dal.dao.ICfgTblProductDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblSOVehicleDetailDAO;
import com.bezkoder.spring.login.sa.dal.dao.ISlsTblWorkOrderDAO;
import com.bezkoder.spring.login.sa.dal.entities.*;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


@RestController
@CrossOrigin( origins = "*" )
public class ProductController {

	private Logger logger = LogManager.getLogger(ProductController.class);

	@Autowired
	private IProductService productService;
	
	@Autowired
	private ICfgTblProductDAO productDAO;
	
	@Autowired
	private ICustomerService customerService;
	
	@Autowired
	private	IProductCategoryService productCategory;
	
	@Autowired
	private	IModelService modelCategory;
	
	@Autowired
	private	ICustomerCategoryService customerCategory;
	
	@Autowired
	private	ICityService cityService;
	
	 @Autowired
	 ICfgTblCustomerDAO customerDAO;
	 
	 @Autowired
	 ISlsTblWorkOrderDAO wo;
	
	@RequestMapping(value = "/getAllProduct", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllProductAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductes()");
		List<CfgTblProduct> products = productService.getAllProduct();
		return products;
	}
	
	@RequestMapping(value = "/getActiveProduct", method = RequestMethod.GET)
	public List<CfgTblProduct> getActiveProduct(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductes()");
		List<CfgTblProduct> products = productService.getActiveProduct();
		return products;
	}

	@RequestMapping(value = "/generateProductNo", method = RequestMethod.GET)
	public String generateProductNo(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productService.generateProductNo("");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	@RequestMapping(value = "/generateProductNowithType", method = RequestMethod.GET)
	public String generateProductNowithType(String type,HttpServletRequest request,
			HttpServletResponse response) {
		try {
			System.out.println("type----------"+type);
			return productService.generateProductNo("Packing");
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}
	
	/*@RequestMapping(value = "/getAllProduct", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllProduct(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProduct()");
		List<CfgTblProduct> products = productService.getAllProduct();
		return products;
	}*/
	
	
	@RequestMapping(value = "/getNewProduct", method = RequestMethod.GET)
	public CfgTblProduct getNewProductAction(HttpServletRequest request, HttpServletResponse response) {
		CfgTblProduct product = new CfgTblProduct();
		return product;
	}

	@RequestMapping(value = "/addNewProduct", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String addNewProductAction(@RequestBody CfgTblProduct citTblProduct, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productService.addNewProduct(citTblProduct);
		} catch (Exception ex) {
			return "{\"status\":\"Failure\"}";
		}
	}

	@RequestMapping(value = "/deleteProduct", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String deleteProductAction(@RequestBody String productesId, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			List<String> idList = new ArrayList<String>();
			for (String id : productesId.split(",")) {
				if (id.isEmpty()) {
					continue;
				}
				idList.add(id);
			}
			return productService.deleteProduct(idList);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			return "Failure";
		}
	}

	@RequestMapping(value = "/updateProduct", method = RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public String updateProductAction(@RequestBody CfgTblProduct citTblProduct, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productService.updateProduct(citTblProduct);
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	
	@RequestMapping(value = "/productExistByProperty", method = RequestMethod.POST)
	public String productExistByPropertyAction(@RequestParam String property,@RequestParam String value,@RequestParam String mode,@RequestParam String customer,@RequestParam String oldValue, HttpServletRequest request,
			HttpServletResponse response) {
		try {
			return productService.getProductByProperty(property, value, mode, oldValue)?"true":"false";
//					productExistByProperty
		} catch (Exception ex) {
			return "Failure";
		}
	}
	
	
	@RequestMapping(value="productBulkUpload",method = RequestMethod.POST)
	public ModelAndView uploadFile(
			@RequestParam("file") MultipartFile file,HttpServletRequest request,RedirectAttributes redirectAttributes,HttpServletResponse response,ModelMap modelMAP) {
			ModelAndView modelAndView = new ModelAndView("product/bulk_product");
		
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
//	            		(ArrayList<String>) productService.uploadFile(rows);
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
	
	
	@RequestMapping(value = "/getAllPacking", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllPackingAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllPacking()");
		List<CfgTblProduct> products = productService.getAllPacking();
		return products;
	}
	
	@RequestMapping(value = "/getAllInventory", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllInventoryAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllInventory()");
//		List<CfgTblProduct> products = productService.getAllInventory();
		List<CfgTblProduct> products = productDAO.getAllSpareParts();
		return products;
	}
	
	@RequestMapping(value="/searchProduct",method=RequestMethod.POST, headers = "Accept=application/json", consumes = MediaType.APPLICATION_JSON_VALUE)
	public List<SPDTO> searchProduct(@RequestBody CfgTblProduct product,HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSaleOrderes()");
		List<SPDTO> lstproduct = productDAO.searchProductSP(product);
		return lstproduct;
		
		
	}
	
	@RequestMapping(value = "/getAllSpareParts", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllSpareParts(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSpareParts()");
		List<CfgTblProduct> products = productDAO.getAllSpareParts();
		return products;
	}
	
	
	
	@RequestMapping(value = "/getAllSalesItem", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllSalesItemAction(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSalesItem()");
		List<CfgTblProduct> products = productService.getAllSalesItem();
		return products;
	}
	
	@RequestMapping(value = "/getAllPurchaseItem", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllPurchaseItem(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllPurchaseItem()");
		List<CfgTblProduct> products = productService.getAllPurchaseItem();
		return products;
	}
	
	@RequestMapping(value = "/getAllImportItem", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllImportItem(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllImportItem()");
		List<CfgTblProduct> products = productService.getAllImportItem();
		return products;
	}
	
	@RequestMapping(value = "/getAllSetItem", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllSetItem(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllSetItem()");
		List<CfgTblProduct> products = productService.getAllSetItem();
		return products;
	}
	
	@RequestMapping(value = "/getAllComponentItem", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllComponentItem(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllComponentItem()");
		List<CfgTblProduct> products = productService.getAllComponentItem();
		return products;
	}
	
//	@RequestMapping(value = "/getAllProductionItem", method = RequestMethod.GET)
//	public List<CfgTblProduct> getAllProductionItem(HttpServletRequest request, HttpServletResponse response) {
//		logger.debug("getAllProductionItem()");
//		List<CfgTblProduct> products = productService.getAllProductionItem();
//		return products;
//	}
	
	@RequestMapping(value = "/getAllProductionItem", method = RequestMethod.GET)
	public List<CfgTblProduct> getAllProductionItem(String priceGroup, HttpServletRequest request, HttpServletResponse response) {
		logger.debug("getAllProductionItem()");
//		String group 
//		if(priceGroup !=null && priceGroup.indexOf("PickUp"))
		
		List<CfgTblProduct> products= new ArrayList();
			 
		if(priceGroup !=null && priceGroup.trim().length() > 0)
			products = productService.getAllProductionItem(priceGroup);
		else
		 products = productService.getAllProductionItem();
		return products;
	}
	
	 @RequestMapping(value = "ProductUpload", method = RequestMethod.POST)
	 public String uploadFile(@RequestParam("serCityId") String serCityId, @RequestParam("file") MultipartFile file,
				HttpServletRequest request, RedirectAttributes redirectAttributes) {
			System.out.println("----------66666---------------");
			ModelAndView model = null;
			if (file.isEmpty()) {

				redirectAttributes.addFlashAttribute("message", "Please select a file to upload");

			}
			List <CfgTblCustomer>  lstCustomers=customerService.getAllCustomer();
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
					// write file to server
					while ((i = is.read()) != -1) {
						stream.write(i);
					}
					stream.flush();
				}
			} catch (IOException e) {
				System.out.println("error while reading csv and put to db : " + e.getMessage());
			}

			try {
				// read file
				// CSVReader(fileReader, ';', '\'', 1) means
				// using separator ; and using single quote ' . Skip first line when
				// read

				try (FileReader fileReader = new FileReader(serverFile);
					 CSVReader reader = new CSVReader(fileReader);) {
					List<String[]> rows = new ArrayList<String[]>();
					rows = reader.readAll();

					for (String[] row : rows) {
						System.out.println(Arrays.toString(row));
					}

					String status = "Success";

					////////////////////////////////////////////////////////////
					
					
					for (String[] arr : rows) {
						arr = arr[0].split(",");
						
						
//						CfgTblProductCategory categoruy=new CfgTblProductCategory();
//						
//						setProductCategory(categoruy, arr,lstCustomers);
//						
//						productCategory.addNewProductCategory(categoruy);
						
						

//						CfgTblModel model1=new CfgTblModel();
//						setProductModel(model1, arr,lstCustomers);
//												
//						modelCategory.addNewModel(model1);

						CfgTblProduct product = new CfgTblProduct();
						setProduct(product, arr,lstCustomers);
						productService.addNewProduct(product);
					

						{
//							cfgTblCustomer.setBoolStatus(true);
//							CfgTblCity city = new CfgTblCity();
//							String id = "0";
//							if (serCityId.contains("string:") || serCityId.contains("number:")) {
//								String array[] = serCityId.split(":");
//								System.out.print(array[1]);
//								id = array[1].toString();
//								city.setSerCityId(Integer.parseInt(id));
//							}

//								 cfgTblCustomer.setCfgTblCity(city);
							
						}
					}

					//////////////////////////////////////////////////////////////

					// setupServiceDao.uploadFile(rows,fkCustomerId,fkContractId,uploadDate);
					if (status.equals("Success")) {
						return "msg=Success&status=" + URLEncoder.encode(status, "UTF-8");
//			        	 return "redirect:/CustomerUpload?msg=Success&status="+URLEncoder.encode(status,"UTF-8");
					} else
						return "msg=error&status=" + URLEncoder.encode(status, "UTF-8");
//			        		 return "redirect:/CustomerUpload?msg=error&status="+URLEncoder.encode(status,"UTF-8");
				} catch (CsvException e) {
					throw new RuntimeException(e);
				}

			} catch (IOException e) {
				System.out.println("error while reading csv and put to db : " + e.getMessage());
			}
			try {
//					return "redirect:/CustomerUpload?msg=error&status="+URLEncoder.encode("error while reading csv and put to db","UTF-8");
				return "msg=error&status=" + URLEncoder.encode("error while reading csv and put to db", "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "error while reading csv and put to db";

		}
	
	  private void setProduct (CfgTblProduct product, String[] arr,List<CfgTblCustomer> lstCustomers)
	  {

			String status="";
			
			if (arr[0].isEmpty()) {
				status = "Customer Code is empty";
//				break;
			} else {

				CfgTblProductCategory cat = new CfgTblProductCategory();
				cat.setTxtProductCategoryCode(arr[0]);
				List<CfgTblProductCategory> lstCat = productCategory.searchProductCategory(cat);
				if (lstCat != null && lstCat.size() > 0)
					product.setCfgTblProductCategory(lstCat.get(0));
			}

			if (arr[1].isEmpty()) {
			} else {
				String code=arr[1];
				product.setTxtProductCode(code);
			}
			
			if (arr[2].isEmpty()) {
			} else {
				String name=arr[2];
				product.setTxtProductName(name);
			}
			
			
			if (arr[3].isEmpty()) {
				
			} else {
				String transmission=arr[3];
				product.setTxtTransmission(transmission);
			}
			
			try {
				if (arr[4].isEmpty()) {
					status = "sku is empty";
					//								break;
				} else {
					String sku=arr[4];
					product.setTxtSKU(sku);
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			

			try {
				if (arr[5].isEmpty()) {
					status = "Customer Name is empty";
					//								break;
				} else {
					String color=arr[5];
					product.setTxtColor(color);
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			
			try {
				if (arr[7].isEmpty()) { 
					status = "Customer Name is empty";
					//								break;
				} else {
					String price=arr[7];
					product.setNumSalePrice(new BigDecimal(Double.parseDouble(price)));
					product.setTxtMachineIp(price);
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			product.setNumSalesTax(new BigDecimal(18));
			
		/*
		 * try { if (arr[8].isEmpty()) { status = "Customer Name is empty"; // break; }
		 * else { String st=arr[9]; product.setNumSalesTax(new
		 * BigDecimal(Double.parseDouble(st))); } } catch
		 * (ArrayIndexOutOfBoundsException e) { // TODO Auto-generated catch block
		 * 
		 * } catch (Exception e) { // TODO: handle exception }
		 * 
		 * 
		 * try { if (arr[10].isEmpty()) { status = "Customer Name is empty"; // break; }
		 * else { String st=arr[10]; product.setNumFED(new
		 * BigDecimal(Double.parseDouble(st))); } } catch
		 * (ArrayIndexOutOfBoundsException e) { // TODO Auto-generated catch block
		 * 
		 * } catch (Exception e) { // TODO: handle exception }
		 * 
		 * 
		 * try { if (arr[11].isEmpty()) { status = "Customer Name is empty"; // break; }
		 * else { String cvt=arr[11]; product.setNumCVT(new
		 * BigDecimal(Double.parseDouble(cvt))); } } catch
		 * (ArrayIndexOutOfBoundsException e) { // TODO Auto-generated catch block
		 * 
		 * } catch (Exception e) { // TODO: handle exception }
		 */
			
			
			
	  }
	  
	  
	  
	  private void setProductCategory(CfgTblProductCategory product, String[] arr,List<CfgTblCustomer> lstCustomers)
	  {
			if (arr[0].isEmpty()) {		
				
			} else {
				String code=arr[0];
				product.setTxtProductCategoryCode(code);
			}
			
			if (arr[1].isEmpty()) {
				
			
				
			} else {
				String name=arr[1];
				product.setTxtProductCategoryName(name);
			}
			
			
			if (arr[2].isEmpty()) {
				
			
				
			} else {
				String transmission=arr[2];
				product.setTxtDescription(transmission);
			}
			
			if (arr[3].isEmpty()) {	
			} else {
				String date=arr[3];
				System.out.println(date);
				product.setTxtMachineIp(date);
			}
	  }
	  
	  
	  
	  private void setProductModel(CfgTblModel product, String[] arr,List<CfgTblCustomer> lstCustomers)
	  {
			if (arr[0].isEmpty()) {		
				
			} else {
				String code=arr[0];
				product.setTxtModelCode(code);
			}
			
			if (arr[1].isEmpty()) {
				
			
				
			} else {
				String name=arr[1];
				product.setTxtModelName(name);
			}
			
			
			if (arr[2].isEmpty()) {
				
			
				
			} else {
				String transmission=arr[2];
				product.setTxtDescription(transmission);
			}
			
			if (arr[3].isEmpty()) {	
			} else {
				String date=arr[3];
				System.out.println(date);
				product.setTxtMachineIp(date);
			}
	  }
	  
		 @RequestMapping(value = "CustomerUpload", method = RequestMethod.POST)
		 public String CustomerUpload(@RequestParam("serCityId") String serCityId, @RequestParam("file") MultipartFile file,
					HttpServletRequest request, RedirectAttributes redirectAttributes) {
				System.out.println("----------66666---------------");
				ModelAndView model = null;
				if (file.isEmpty()) {

					redirectAttributes.addFlashAttribute("message", "Please select a file to upload");

				}
				List <CfgTblCustomer>  lstCustomers=customerService.getAllCustomer();
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
						// write file to server
						while ((i = is.read()) != -1) {
							stream.write(i);
						}
						stream.flush();
					}
				} catch (IOException e) {
					System.out.println("error while reading csv and put to db : " + e.getMessage());
				}

				try {
					// read file
					// CSVReader(fileReader, ';', '\'', 1) means
					// using separator ; and using single quote ' . Skip first line when
					// read

					try (FileReader fileReader = new FileReader(serverFile);
						CSVReader reader = new CSVReader(fileReader)) {
						List<String[]> rows = new ArrayList<String[]>();
						rows = reader.readAll();

						for (String[] row : rows) {
							System.out.println(Arrays.toString(row));
						}

						String status = "Success";

						////////////////////////////////////////////////////////////
						
						
						for (String[] arr : rows) {
							arr = arr[0].split(",");
							CfgTblCustomer customer = new CfgTblCustomer();

							setCustomer(customer, arr,lstCustomers);
							
						

							{
								customerService.addNewCustomer(customer);
							}
						}

						//////////////////////////////////////////////////////////////

						// setupServiceDao.uploadFile(rows,fkCustomerId,fkContractId,uploadDate);
						if (status.equals("Success")) {
							return "msg=Success&status=" + URLEncoder.encode(status, "UTF-8");
//				        	 return "redirect:/CustomerUpload?msg=Success&status="+URLEncoder.encode(status,"UTF-8");
						} else
							return "msg=error&status=" + URLEncoder.encode(status, "UTF-8");
//				        		 return "redirect:/CustomerUpload?msg=error&status="+URLEncoder.encode(status,"UTF-8");
					} catch (CsvException e) {
						throw new RuntimeException(e);
					}

				} catch (IOException e) {
					System.out.println("error while reading csv and put to db : " + e.getMessage());
				}
				try {
//						return "redirect:/CustomerUpload?msg=error&status="+URLEncoder.encode("error while reading csv and put to db","UTF-8");
					return "msg=error&status=" + URLEncoder.encode("error while reading csv and put to db", "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return "error while reading csv and put to db";

			}
			NumberFormat formatter = new DecimalFormat("###");
	  private void setCustomer (CfgTblCustomer customer, String[] arr,List<CfgTblCustomer> lstCustomers)
	  {
//		  CfgTblProductCategory cat=new CfgTblProductCategory();
//			cat.setSerCategoryId(2);
//			
//			product.setCmsTableProductCategory(cat);
			String status="";
			
			if (arr[0].isEmpty()) {
				status = "Customer Code is empty";
//				break;
			} else {

				CfgTblCustomerCategory cat = new CfgTblCustomerCategory();
				cat.setTxtCustomerCategoryCode(arr[0]);
				List<CfgTblCustomerCategory> lstCat = customerCategory.searchCustomerCategory(cat);
				if (lstCat != null && lstCat.size() > 0)
					customer.setCfgTblCustomerCategory(lstCat.get(0));
			}

			if (arr[1].isEmpty()) {
				
			
				
			} else {
				String code=arr[1];
				customer.setTxtCustomerCode(code);
			}
			
			if (arr[2].isEmpty()) {
				
			
				
			} else {
				String name=arr[2];
				customer.setTxtCustomerName(name);
			}
			
			
			if (arr[3].isEmpty()) {
				
			
				
			} else {
				String fnamer=arr[3];
				customer.setTxtFName(fnamer);
			}
			
if (arr[4].isEmpty()) {
				
			
				
			} else {
				String email=arr[4];
				customer.setTxtEmailAddress(email);
			}

//			try {
//				if (arr[5].isEmpty()) {
//					status = "sku is empty";
//					//								break;
//				} else {
//					String sku=arr[5];
//					customer.setTxtdSKU(sku);
//				} 
//			} 
//			catch (ArrayIndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//
//			}
//			catch (Exception e) {
//				// TODO: handle exception
//			}
			
			

			try {
				if (arr[6].isEmpty()) {
					status = "cnic  is empty";
					//								break;
				} else {
					String cnic=arr[6];
					Double d=Double.parseDouble(cnic.replace('\"', ' ').trim());
					customer.setTxtCnicNo(formatter.format(d).toString());
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			
			try {
				if (arr[7].isEmpty()) {
					status = "cnic  is empty";
					//								break;
				} else {
					String cnic=arr[7];
					Double d=Double.parseDouble(cnic.replace('\"', ' ').trim());
					customer.setTxtPhoneNo(formatter.format(d).toString());
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			
			try {
				if (arr[8].isEmpty()) {
					status = "cnic  is empty";
					//								break;
				} else {
				CfgTblCity cat = new CfgTblCity();
				cat.setTxtCityName(arr[8].toUpperCase());
				List<CfgTblCity> lstCat = cityService.searchCity(cat);
				if (lstCat != null && lstCat.size() > 0)
					customer.setCfgTblCity(lstCat.get(0));
				else
					{
						CfgTblCity city=new CfgTblCity();
						city.setTxtCityCode(arr[8].toUpperCase());
						city.setTxtCityName(arr[8].toUpperCase());
						CfgTblCountry cfgTblCountry=new CfgTblCountry();
						cfgTblCountry.setSerCountryId(3494);
						city.setCfgTblCountry(cfgTblCountry);
						cityService.addNewCity(city);
						
						
						lstCat = cityService.searchCity(cat);
						if (lstCat != null && lstCat.size() > 0)
							customer.setCfgTblCity(lstCat.get(0));
					}
				}
				
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			
			try {
				if (arr[11].isEmpty()) {
					status = "Customer Name is empty";
					//								break;
				} else {
					String ntn=arr[11];
					customer.setTxtNtnNo(ntn);
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			
			try {
				if (arr[14].isEmpty()) {
					status = "Customer Name is empty";
					//								break;
				} else {
					String st=arr[14];
					if( Integer.parseInt(st)==1)
						customer.setBlnIsFiler(true);
					else
						customer.setBlnIsFiler(false);
				
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			
			try {
				if (arr[16].isEmpty()) {
				
					//								break;
				} else {
					String address=arr[16];
					customer.setTxtDisplayAddress(address);
				} 
			} 
			catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block

			}
			catch (Exception e) {
				// TODO: handle exception
			}
			
			customer.setBlnCommercial(false);
			customer.setBlnPassanger(false);
			CfgTblCountry cfgTblCountry=new CfgTblCountry();
			cfgTblCountry.setSerCountryId(3494);
			customer.setCfgTblCountry(cfgTblCountry);
			customer.setBlIsDealer(false);
			
	  }
	  
	  /**
	   * 
	   * 
	   * 
	   * 
	   * 
	   */
	  
	  @Autowired	
		private ISlsTblSOVehicleDetailDAO vehicleDetailService;
		
	  
	  @RequestMapping(value = "JCUpload", method = RequestMethod.POST)
		 public String JCUpload(@RequestParam("serCityId") String serCityId, @RequestParam("file") MultipartFile file,
					HttpServletRequest request, RedirectAttributes redirectAttributes) {
				System.out.println("----------66666---------------");
				ModelAndView model = null;
				if (file.isEmpty()) {

					redirectAttributes.addFlashAttribute("message", "Please select a file to upload");

				}
				List <CfgTblCustomer>  lstCustomers=customerService.getAllCustomer();
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
						// write file to server
						while ((i = is.read()) != -1) {
							stream.write(i);
						}
						stream.flush();
					}
				} catch (IOException e) {
					System.out.println("error while reading csv and put to db : " + e.getMessage());
				}

					// read file
					// CSVReader(fileReader, ';', '\'', 1) means
					// using separator ; and using single quote ' . Skip first line when
					// read

					try (FileReader fileReader = new FileReader(serverFile);
						CSVReader reader = new CSVReader(fileReader)) {
						List<String[]> rows = new ArrayList<String[]>();
						rows = reader.readAll();

						for (String[] row : rows) {
							System.out.println(Arrays.toString(row));
						}

						String status = "Success";

						////////////////////////////////////////////////////////////
						int count=0;
						
						for (String[] arr : rows) {
							if(count>520)
								break;
							count++;
							arr = arr[0].split(",");
							CfgTblCustomer customer = new CfgTblCustomer();

							
							
							SlsTblSoVehicleDetail vd=new SlsTblSoVehicleDetail();
							setVehicleDetail(vd, arr,lstCustomers);
							
							SlsTblSoVehicleDetail vd_exist=vehicleDetailService.getVehicleDetailByChassisno(vd.getTxtChassisNo());
							if(vd_exist == null)
							vd = vehicleDetailService.addNewVehicleDetailRVC(vd);
							else
								vd=vd_exist;
							
							
							SlsTblWorkOrder workorder=wo.getWorkOrderByCode(vd.getTxtInvoiceNo());
							
							if(workorder == null)
							{
								SlsTblWorkOrder swo=new SlsTblWorkOrder();
								swo.setCfgTblCustomer(vd.getCfgTblCustomer());
								swo.setCfgTblDealer(vd.getCfgTblDealer());
								swo.setCfgTblProduct(vd.getCfgTblProduct());
								swo.setCfgTblModel(vd.getCfgTblModel());
								swo.setSlsTblSoVehicleDetail(vd);
								swo.setTxtWorkOrderNo(vd.getTxtInvoiceNo());
								CfgTblJobCategory cfgTblJobCategory=new CfgTblJobCategory();
								if (arr[19].trim().equalsIgnoreCase("First Free Service")) {
									cfgTblJobCategory.setSerJobCategoryId(3752);
								}else if (arr[19].trim().equalsIgnoreCase("Second Free Service")) {
									cfgTblJobCategory.setSerJobCategoryId(3);
								} else if (arr[19].trim().equalsIgnoreCase("Third Free Service")) {
									cfgTblJobCategory.setSerJobCategoryId(4);
								} else if (arr[19].trim().equalsIgnoreCase("Body Repair")) {
									cfgTblJobCategory.setSerJobCategoryId(3746);
								} else if (arr[19].trim().equalsIgnoreCase("Warranty Repair")) {
									cfgTblJobCategory.setSerJobCategoryId(5);
								} else if (arr[19].trim().equalsIgnoreCase("Warranty Repair Warranty Repair Service")) {
									cfgTblJobCategory.setSerJobCategoryId(3);
								} else if (arr[19].trim().equalsIgnoreCase("Oil & Oil Filter Change Only")) {
									cfgTblJobCategory.setSerJobCategoryId(3753);
								} else if (arr[19].trim().equalsIgnoreCase("Pre Delivery Inspection")) {
									cfgTblJobCategory.setSerJobCategoryId(3755);
								} else if (arr[19].trim().equalsIgnoreCase("Scheduled PM")) {
									cfgTblJobCategory.setSerJobCategoryId(3771);
								}else if (arr[19].trim().equalsIgnoreCase("20,000 KM")) {
									cfgTblJobCategory.setSerJobCategoryId(8);
								}else if (arr[19].trim().equalsIgnoreCase("20,000 KM")) {
									cfgTblJobCategory.setSerJobCategoryId(9);
								}else {
									cfgTblJobCategory.setSerJobCategoryId(3754);
								}
								
								swo.setCfgTblJobCategory(cfgTblJobCategory);
								String Rec = arr[21].toString()+" "+arr[22].toString();
								swo.setTxtTimeIn(arr[22].toString());
								swo.setTxtDriver(Rec);
								try {
									swo.setNumCurrentMillage(new BigDecimal(Double.parseDouble(arr[18])));
								} catch (NumberFormatException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								wo.addNewWorkOrder(swo);
							}
						//////////////////////////////////////////////////////////////

						// setupServiceDao.uploadFile(rows,fkCustomerId,fkContractId,uploadDate);
						if (status.equals("Success")) {
//							return "msg=Success&status=" + URLEncoder.encode(status, "UTF-8");
////				        	 return "redirect:/CustomerUpload?msg=Success&status="+URLEncoder.encode(status,"UTF-8");
//						} else
//							return "msg=error&status=" + URLEncoder.encode(status, "UTF-8");
//				        		 return "redirect:/CustomerUpload?msg=error&status="+URLEncoder.encode(status,"UTF-8");
					}
					}
				} catch (IOException | CsvException e) {
					System.out.println("error while reading csv and put to db : " + e.getMessage());
				}
				try {
//						return "redirect:/CustomerUpload?msg=error&status="+URLEncoder.encode("error while reading csv and put to db","UTF-8");
					return "msg=error&status=" + URLEncoder.encode("error while reading csv and put to db", "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				return "error while reading csv and put to db";
return null;
			}
		
	  private void setVehicleDetail (SlsTblSoVehicleDetail vd, String[] arr,List<CfgTblCustomer> lstCustomers)
	  {
			String status="";
			
		
			
			
vd.setTxtEngineNo("NA");
if (arr[5].isEmpty()) {	
} else {
	String name=arr[5];
	vd.setTxtRegistrationNo(name);
	
	System.out.println(name+"--------");
}
if(arr[6] !=  null)
if (  arr[6].isEmpty()) {	
} else {
	String name=arr[6];
	vd.setTxtChassisNo(name);
}

if(arr[7] !=  null)
if (arr[7].isEmpty()) {	
} else {
	String name=arr[7];
	vd.setTxtTransmission(name);
}

vd.setTxtInvoiceNo(arr[0]);

vd.setDteInvoiceDate(new Date());
vd.setDteDeliveryDate(new Date());
vd.setTxtMachineIp(arr[1]);

CfgTblCustomer cus=new CfgTblCustomer();
cus.setTxtCustomerCode(vd.getTxtChassisNo());
cus.setTxtCustomerName(arr[3]);
CfgTblCustomer cus_exit = customerDAO.getCustomerBycode(vd.getTxtChassisNo());

if(cus_exit!=null)
	vd.setCfgTblCustomer(cus_exit);
else
{
cus = customerDAO.addNewCustomerRC(cus);
vd.setCfgTblCustomer(cus);
}


CfgTblCustomer dealer = new CfgTblCustomer();
//dealer.setSerCustomerId(1125);  //Cherry Centeral 
//dealer.setSerCustomerId(1124);  //Cherry Gujrat 

//dealer.setSerCustomerId(1122);  //Cherry city 


//dealer.setSerCustomerId(1124);  //Cherry Gujrat

//dealer.setSerCustomerId(1127);  //Cherry Lylpur

//dealer.setSerCustomerId(1128);  //Cherry Islamabad

//dealer.setSerCustomerId(1121);  //Cherry Multan

dealer.setSerCustomerId(1123);  //Cherry gulberg


vd.setCfgTblDealer(dealer);

CfgTblProduct product=new CfgTblProduct();
product.setSerProductId(11);
vd.setCfgTblProduct(product);

//			if (arr[0].isEmpty()) {
//				status = "Customer Code is empty";
////				break;
//			} else {
//searchWorkOrder
//				CfgTblCustomerCategory cat = new CfgTblCustomerCategory();
//				cat.setTxtCustomerCategoryCode(arr[0]);
//				List<CfgTblCustomerCategory> lstCat = customerCategory.searchCustomerCategory(cat);
//				if (lstCat != null && lstCat.size() > 0)
//					customer.setCfgTblCustomerCategory(lstCat.get(0));
//			}
//
//			if (arr[1].isEmpty()) {
//				
//			
//				
//			} else {
//				String code=arr[1];
//				customer.setTxtCustomerCode(code);
//			}
//			
//			if (arr[2].isEmpty()) {
//				
//			
//				
//			} else {
//				String name=arr[2];
//				customer.setTxtCustomerName(name);
//			}
//			
//			
//			if (arr[3].isEmpty()) {
//				
//			
//				
//			} else {
//				String fnamer=arr[3];
//				customer.setTxtFName(fnamer);
//			}
//			
//if (arr[4].isEmpty()) {
//				
//			
//				
//			} else {
//				String email=arr[4];
//				customer.setTxtEmailAddress(email);
//			}
//
////			try {
////				if (arr[5].isEmpty()) {
////					status = "sku is empty";
////					//								break;
////				} else {
////					String sku=arr[5];
////					customer.setTxtdSKU(sku);
////				} 
////			} 
////			catch (ArrayIndexOutOfBoundsException e) {
////				// TODO Auto-generated catch block
////
////			}
////			catch (Exception e) {
////				// TODO: handle exception
////			}
//			
//			
//
//			try {
//				if (arr[6].isEmpty()) {
//					status = "cnic  is empty";
//					//								break;
//				} else {
//					String cnic=arr[6];
//					Double d=Double.parseDouble(cnic.replace('\"', ' ').trim());
//					customer.setTxtCnicNo(formatter.format(d).toString());
//				} 
//			} 
//			catch (ArrayIndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//
//			}
//			catch (Exception e) {
//				// TODO: handle exception
//			}
//			
//			
//			try {
//				if (arr[8].isEmpty()) {
//					status = "cnic  is empty";
//					//								break;
//				} else {
//				CfgTblCity cat = new CfgTblCity();
//				cat.setTxtCityName(arr[8].toUpperCase());
//				List<CfgTblCity> lstCat = cityService.searchCity(cat);
//				if (lstCat != null && lstCat.size() > 0)
//					customer.setCfgTblCity(lstCat.get(0));
//				else
//					{
//						CfgTblCity city=new CfgTblCity();
//						city.setTxtCityCode(arr[8].toUpperCase());
//						city.setTxtCityName(arr[8].toUpperCase());
//						CfgTblCountry cfgTblCountry=new CfgTblCountry();
//						cfgTblCountry.setSerCountryId(3494);
//						city.setCfgTblCountry(cfgTblCountry);
//						cityService.addNewCity(city);
//						
//						
//						lstCat = cityService.searchCity(cat);
//						if (lstCat != null && lstCat.size() > 0)
//							customer.setCfgTblCity(lstCat.get(0));
//					}
//				}
//				
//			} 
//			catch (ArrayIndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//
//			}
//			catch (Exception e) {
//				// TODO: handle exception
//			}
//			
//			
//			try {
//				if (arr[11].isEmpty()) {
//					status = "Customer Name is empty";
//					//								break;
//				} else {
//					String ntn=arr[11];
//					customer.setTxtNtnNo(ntn);
//				} 
//			} 
//			catch (ArrayIndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//
//			}
//			catch (Exception e) {
//				// TODO: handle exception
//			}
//			
//			
//			try {
//				if (arr[14].isEmpty()) {
//					status = "Customer Name is empty";
//					//								break;
//				} else {
//					String st=arr[14];
//					if( Integer.parseInt(st)==1)
//						customer.setBlnIsFiler(true);
//					else
//						customer.setBlnIsFiler(false);
//				
//				} 
//			} 
//			catch (ArrayIndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//
//			}
//			catch (Exception e) {
//				// TODO: handle exception
//			}
//			
//			
//			try {
//				if (arr[16].isEmpty()) {
//				
//					//								break;
//				} else {
//					String address=arr[16];
//					customer.setTxtDisplayAddress(address);
//				} 
//			} 
//			catch (ArrayIndexOutOfBoundsException e) {
//				// TODO Auto-generated catch block
//
//			}
//			catch (Exception e) {
//				// TODO: handle exception
//			}
//			
//			customer.setBlnCommercial(false);
//			customer.setBlnPassanger(false);
//			CfgTblCountry cfgTblCountry=new CfgTblCountry();
//			cfgTblCountry.setSerCountryId(3494);
//			customer.setCfgTblCountry(cfgTblCountry);
//			customer.setBlIsDealer(true);
			
	  }
	   
}
