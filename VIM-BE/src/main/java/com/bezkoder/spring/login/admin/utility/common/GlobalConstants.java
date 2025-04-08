/**
 * 
 */
package com.bezkoder.spring.login.admin.utility.common;

import java.util.Arrays;
import java.util.List;

/**
 * @author Muhammad Khalil
 *
 */
public class GlobalConstants {

	/*----------------Database Related Constants-----------------
	public static final String			PERSISTANCE_UNIT								= "persistane.unit";

	public static final String			QUERY_SELECT_ALL_WHERE_ORDER					= "select.all.where.order";
	public static final String			QUERY_SELECT_ALL_WHERE_AND_ORDER				= "select.all.where.and.order";
	public static final String			QUERY_SELECT_ALL								= "select.all";
	public static final String			QUERY_SELECT_ALL_UNAUTH							= "select.all.unauth";
	public static final String			QUERY_SELECT_ALL_AUTH							= "select.all.auth";
	public static final String			QUERY_SELECT_ALL_WHERE							= "select.all.where";
	public static final String			QUERY_SELECT_ALL_WHERE_AND						= "select.all.where.and";
	public static final String			QUERY_DELETE_ALL_WHERE							= "delete.all.where";

	public static final String			ENTITY_TBS_SETUP_COMPANY						= "TbsSetupCompany";
	public static final String			ENTITY_TBS_SETUP_COUNTRY						= "TbsSetupCountry";
	public static final String			ENTITY_TBS_SETUP_CITY							= "TbsSetupCity";
	public static final String			ENTITY_TBS_SETUP_INDUSTRY						= "TbsSetupIndustry";
	public static final String			ENTITY_TBS_SETUP_BASE_LOCATION					= "TbsSetupBaseLocation";
	public static final String			ENTITY_TBS_SETUP_AREA							= "TbsSetupArea";
	public static final String			ENTITY_TBS_SETUP_REGION							= "TbsSetupRegion";
	public static final String			ENTITY_TBS_SETUP_BANK							= "TbsSetupBank";
	public static final String			ENTITY_TBS_SETUP_BRANCH							= "TbsSetupBranch";
	public static final String			ENTITY_TBS_SETUP_OFFICER						= "TbsSetupOfficer";
	public static final String			ENTITY_TBS_SETUP_EMPLOYEE_CATEGORY				= "TbsSetupEmployeeCategory";
	public static final String			ENTITY_TBS_SETUP_CURRENCY						= "TbsSetupCurrency";
	public static final String			ENTITY_TBS_SETUP_BANK_GROUP						= "TbsSetupBankGroup";
	public static final String			ENTITY_TBS_SETUP_PAYMENTMODE					= "TbsSetupPaymentmode";
	public static final String			ENTITY_TBS_SETUP_HOLIDAY						= "TbsSetupHoliday";
	public static final String			ENTITY_TBS_SETUP_DIVISION						= "TbsSetupDivision";
	public static final String			ENTITY_TBS_SETUP_GROUP							= "TbsSetupGroup";
	public static final String			ENTITY_TBS_SETUP_UNIT							= "TbsSetupUnit";
	public static final String			ENTITY_TBS_SETUP_ACCOUNT						= "TbsSetupAccount";
	public static final String			ENTITY_TBS_SETUP_RETURN_REASON					= "TbsSetupReturnReason";
	public static final String			ENTITY_APP_ROLE									= "AppRole";
	public static final String			ENTITY_APP_USER									= "AppUser";
	public static final String			ENTITY_TBS_SETUP_STATIONARY						= "TbsSetupStationary";
	public static final String			ENTITY_TBS_SETUP_TAX_CATEGORY					= "TbsSetupTaxCategory";
	public static final String			ENTITY_TBS_SETUP_INSTRUMENT						= "TbsSetupInstrument";
	public static final String			ENTITY_TBS_SETUP_COMPANY_DEALER					= "TbsSetupCompanyDealer";
	public static final String			ENTITY_TBS_SETUP_COURIER						= "TbsSetupCourier";
	public static final String			ENTITY_TBS_SETUP_PRODUCT						= "TbsSetupProduct";
	public static final String			ENTITY_TBS_SETUP_COMPANY_DEALER_ACCTS			= "TbsSetupCompanyDealerAcct";
	public static final String			ENTITY_APP_INTERFACE							= "AppInterface";
	public static final String			ENTITY_APP_MODULE								= "AppModule";
	public static final String			ENTITY_APP_MODULE_INTERFACE						= "AppModuleInterface";
	public static final String			ENTITY_APP_ROLE_AUTHORIZATION					= "AppRoleAuthorization";
	public static final String			ENTITY_APP_ROLE_MODULE							= "AppRoleModule";
	public static final String			ENTITY_APP_ACTIVITY_LOG							= "AppActivityLog";
	public static final String			ENTITY_TBS_SETUP_COMPANY_PRODUCT_ARR			= "TbsSetupCompanyProductArr";
	public static final String			ENTITY_TBS_SETUP_CORR_BANK_ARR					= "TbsSetupCorrBankArr";
	public static final String			ENTITY_TBS_SETUP_DEPOSIT_CHANNEL				= "TbsSetupDepositChannel";
	public static final String			ENTITY_TBS_CONFIG_DEFMASTER						= "TbsConfigDefMaster";
	public static final String			ENTITY_TBS_CONFIG_DEFDETAIL						= "TbsConfigDefDetail";
	public static final String			ENTITY_TBS_CUSTOMER_CONF_LINKAGE				= "TbsCustomerConfLinkage";
	public static final String			ENTITY_TBS_DATA_CUSTOMIZATION_MASTER			= "TbsDataCustomizationMaster";
	public static final String			ENTITY_TBS_SIGNATORY_SETUP						= "TbsSignatorySetup";
	public static final String			ENTITY_TBS_DATA_CUSTOMIZATION_DETAIL			= "TbsDataCustomizationDetail";
	public static final String			ENTITY_TBS_PSO_RPR_MASTER						= "TbsPsoRprMaster";
	public static final String			ENTITY_TBS_PSO_RPR_DETAIL						= "TbsPsoRprDetail";
	public static final String			ENTITY_TBS_CUSTOMER_INSTRUMENT_SETUP			= "TbsCustomerInstrumentSetup";
	public static final String			ENTITY_TBS_OBC_MASTER							= "TbsObcMaster";
	public static final String			ENTITY_TBS_UPLOAD_INVOICES						= "TbsUploadInvoice";
	public static final String			ENTITY_TBS_RAW_DATA_UPLOAD						= "TbsRawDataload1";
	public static final String			ENTITY_TBS_INSTRUMENT_FILE_SETUP				= "TbsInstrumentFileSetup";
	public static final String			ENTITY_TBS_AUTHORIZATION_MATRIX					= "TbsAuthorizationMatrix";
	public static final String			ENTITY_TBS_AUTH_MATRIX_DETAIL					= "TbsAuthMatrixDetail";
	public static final String			ENTITY_TBS_ACCOUNT_PRODUCT_SETUP				= "TbsAccountProductSetup";
	public static final String			ENTITY_TBS_FILELOG								= "TbsFilelog";
	public static final String			ENTITY_TBS_UTILITY_MASTER_DATA					= "TbsUtilityMasterData";
	public static final String			ENTITY_TBS_UTILITY_MONTHLY_DATA					= "TbsUtilityMonthlyData";
	public static final String			ENTITY_TBS_UBCS_OFFLINE_UPLOAD_MASTER			= "TbsUbcsOfflineUploadMaster";
	public static final String			ENTITY_TBS_UBCS_OFFLINE_UPLOAD_DETAIL			= "TbsUbcsOfflineUploadDetail";
	public static final String			ENTITY_TBS_CONFIG_Detail				= "TbsConfigDetail";
	public static final String          ENTITY_TBS_Raw_Dataload                         ="RawDataload2";

	public static final String			ENTITY_TBS_KEYWORD_DICTIONARY					= "TbsKeywordDictionary";
	public static final String			ENTITY_TBS_RAW_DATA_UPLOAD_TEMP					= "TbsRawDataload1Temp";
	public static final String			ENTITY_TBS_RPS_BENEFICIARY						= "TbsRpsBeneficiary";
	public static final String			ENTITY_TBS_RPS_DRAFT							= "TbsRpsDraft";

	public static final String			PROJECT_SECURITY_KEY							= "project_security_key";
	public static final String			BULK_INSTRUCTION_FILE_UPLOAD_LOCATION			= "bulk_instruction_file_upload_location";
	public static final String			INDICATOR_NO									= "N";
	public static final String			INDICATOR_YES									= "Y";
	// public static final String PROCESS_AUTH = "AUTH";
	public static final String			PROCESS_INSERT									= "CREATE";
	public static final String			PROCESS_SELECT									= "READ";
	public static final String			PROCESS_UPDATE									= "UPDATE";
	public static final String			PROCESS_DELETE									= "DELETE";
	public static final String			FILE_TYPE_EXCEL									= "0";
	public static final String			FILE_TYPE_CSV									= "1";
	public static final String			FILE_TYPE_TXT									= "2";
	public static final String			ENTITY_TBS_OFFLINE_UPLOAD_LOG_MASTER			= "TbsOfflineUploadLogMaster";
	public static final String			ENTITY_TBS_OFFLINE_UPLOAD_LOG_DETAIL			= "TbsOfflineUploadLogDetail";
	public static final String			ENTITY_TBS_DEPOSIT_MASTER						= "TbsDepositMaster";
	public static final String			ENTITY_TBS_DEPOSIT_REFERENCE_DETAIL				= "TbsDepositReferenceDetail";
	public static final String			ENTITY_TBS_DEPOSIT_CHECK_DETAIL					= "TbsDepositCheckDetail";
	public static final String			ENTITY_TBS_DEPOSIT_DETAIL						= "TbsDepositDetail";

	----------------Application Transaction Type Constants-----------------

	public static final String			TRAN_TYPE_COC									= "COC";
	public static final String			TRAN_TYPE_DD									= "DD";
	public static final String			TRAN_TYPE_IBFT									= "IBFT";
	public static final String			TRAN_TYPE_A2A									= "A2A";
	public static final String			TRAN_TYPE_PRI									= "PRI";
	// public static final String TRAN_TYPE_DRAFT = "DRAFT";

	----------------Application Modules Constants-----------------

	---------------- Web Services Constants -----------------

	public static final String			BILLING_SERVICE_MCB_USERNAME					= "billing.service.mcb.username";
	public static final String			BILLING_SERVICE_MCB_PASSWORD					= "billing.service.mcb.password";
	public static final String			BILLING_SERVICE_MCB_IP_ADDRESS					= "billing.service.mcb.ipaddress";
	public static final String			BILLING_SERVICE_MCB_ATMID						= "5555";
	public static final String			BILLING_SERVICE_MCB_IMDCODE						= "555555";
	public static final String			BILLING_SERVICE_MCB_SEQNO						= "1000";
	public static final String			BILLING_SERVICE_MCB_TRANSACTION_CHANNEL			= "TBD";
	public static final String			BILLING_SERVICE_MCB_CUSTOMER_TEMPORARY_ACCOUNT	= "1236547891236547";

	public static final String			MODULE_MIS										= "MIS";
	public static final String			MODULE_ADMIN									= "ADMIN";
	public static final String			MODULE_PAYMENT									= "PAYMENT";
	public static final String			MODULE_COLLECTION								= "COLLECTION";
	public static final String			MODULE_BRANCH_TELLER							= "BRANCH_TELLER";
	public static final String			MODULE_CUSTOMER_PORTAL							= "CUSTOMER_PORTAL";

	private static final List<String>	excludedInterfacesList							= Arrays.asList("dashboard",
			"404", "403", "login", "logout", "error");

	public static final List<String> getExcludedinterfaceslist() {
		return excludedInterfacesList;
	}*/

}
