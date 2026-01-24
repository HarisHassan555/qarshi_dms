# `/getAllDealer` Endpoint Documentation

## Overview
The `/getAllDealer` endpoint retrieves all active dealers from the system. Dealers are customers marked with the `blIsDealer = TRUE` flag.

## Endpoint Details
- **URL**: `/VIM/getAllDealer`
- **Method**: `GET`
- **Content-Type**: `application/json`
- **Response**: `List<CfgTblCustomer>`
- **Authentication**: Required (based on logged-in user context)

## Database Query
The endpoint executes the following query:
```sql
SELECT * FROM cfg_tbl_customer 
WHERE bl_is_dealer = TRUE 
  AND bl_is_deleted = FALSE
```

## Response Data Structure

The endpoint returns an array of `CfgTblCustomer` objects with the following fields:

### **Primary Identifiers**
- `serCustomerId` (Integer) - Unique customer/dealer ID
- `txtCustomerCode` (String) - Dealer code
- `txtCustomerName` (String) - Dealer name
- `txtBusinessName` (String) - Business name
- `txtInvoiceName` (String) - Name to appear on invoices
- `txtSapNo` (String) - SAP system reference number
- `txtUserName` (String) - Username for dealer account

### **Status & Flags**
- `blIsDeleted` (Boolean) - Deletion flag (always `false` in response)
- `blIsDealer` (Boolean) - Dealer flag (always `true` in response)
- `blnStatus` (Boolean) - Active/Inactive status
- `blIsGroup` (Boolean) - Group customer flag
- `blIsAccountExist` (Boolean) - Account existence flag
- `blIsLabsa` (Boolean) - LABSA flag
- `blnIsFiler` (Boolean) - Tax filer status
- `blnIsGst` (Boolean) - GST registration status
- `blnCommercial` (Boolean) - Commercial customer flag
- `blnPassanger` (Boolean) - Passenger vehicle flag
- `blnIsExport` (Boolean) - Export customer flag
- `blIsPOSTEDToSAP` (Boolean) - SAP sync status

### **Contact Information**
- `txtEmailAddress` (String) - Email address
- `txtMobileNo` (String) - Mobile number
- `txtPhoneNo` (String) - Primary phone number
- `txtPhoneNo2` (String) - Secondary phone number
- `txtCnicNo` (String) - CNIC number

### **Address Information**
- `txtBillingAddress` (String) - Billing address
- `txtShippingAddress` (String) - Shipping address
- `txtDisplayAddress` (String) - Display address
- `txtProvince` (String) - Province name

### **Tax & Registration Information**
- `txtNtnNo` (String) - NTN (National Tax Number)
- `txtGstNumber` (String) - GST number
- `txtGstNameOnInvoice` (String) - GST name for invoice
- `txtSTR` (String) - Sales Tax Registration number
- `txtFTN` (String) - FTN number
- `txtIsFiler` (String) - Filer status text
- `numDiscount` (BigDecimal) - Discount percentage
- `numSalesTax` (BigDecimal) - Sales tax percentage
- `numExciseDuty` (BigDecimal) - Excise duty percentage
- `numFED` (BigDecimal) - Federal Excise Duty percentage
- `numFurtherTax` (BigDecimal) - Further tax percentage

### **Date Fields**
- `dteCreateddate` (Timestamp) - Creation timestamp
- `dteModifieddate` (Timestamp) - Last modification timestamp
- `dteDOB` (Date) - Date of birth
- `dteExpiryDate` (Date) - Expiry date
- `txtExpiryDate` (String) - Expiry date (text format)

### **Geographic Relationships**
- `cfgTblCity` (Object) - City information
  - `serCityId`, `txtCityName`, etc.
- `cfgTblCountry` (Object) - Country information
  - `serCountryId`, `txtCountryName`, etc.
- `cfgTblArea` (Object) - Area information
- `cfgTblRegion` (Object) - Region information
- `cfgTblZone1` (Object) - Zone 1 information
- `cfgTblZone2` (Object) - Zone 2 information
- `cfgTblZone3` (Object) - Zone 3 information

### **Category & Classification**
- `cfgTblCustomerCategory` (Object) - Customer category
  - `serCustomerCategoryId`, `txtCustomerCategoryName`, `txtCustomerCategoryCode`, etc.
- `cfgTblDivision` (Object) - Division information
- `cfgTblIncoTerm` (Object) - Incoterms information

### **Hierarchical Relationships**
- `cfgTblCustomer` (Object) - Parent customer reference
  - `serCustomerId` - Parent customer ID
- `cfgTblGroupCustomer` (Object) - Group customer reference
  - `serCustomerId` - Group customer ID

### **Employee Assignment**
- `hrTblEmployee` (Object) - Assigned employee
  - `serEmployeeId`, `txtEmployeeName`, etc.

### **System & Audit Fields**
- `serCreatedUserId` (Integer) - User who created the record
- `serModifiedUserId` (Integer) - User who last modified
- `serGroupId` (Integer) - Group/team identifier
- `txtMachineIp` (String) - Machine IP address
- `txtDivision` (String) - Division text

### **HOD (Head of Department) Information**
- `txtDesignation` (String) - Designation
- `txtHOD` (String) - Head of Department name
- `txtHODMobile` (String) - HOD mobile number
- `txtHODLandLine` (String) - HOD landline
- `txtHODEmailAddress` (String) - HOD email

### **SAP Integration Fields**
- `txtXMSent` (String) - XML sent to SAP
- `txtXMReceive` (String) - XML received from SAP
- `txtReturnMsg` (String) - Return message from SAP
- `txtErrorMsgFromSap` (String) - Error message from SAP

### **Other Fields**
- `txtFName` (String) - First name
- `slsTblSaleOrders` (List) - Related sale orders (lazy loaded, may not be included in response)

## Sample Response

```json
[
  {
    "serCustomerId": 1,
    "txtCustomerCode": "DLR-001",
    "txtCustomerName": "ABC Dealers",
    "txtBusinessName": "ABC Trading Company",
    "txtInvoiceName": "ABC Dealers",
    "txtEmailAddress": "contact@abcdealers.com",
    "txtMobileNo": "03001234567",
    "txtPhoneNo": "021-1234567",
    "txtBillingAddress": "123 Main Street, Karachi",
    "txtShippingAddress": "123 Main Street, Karachi",
    "blIsDealer": true,
    "blnStatus": true,
    "blIsDeleted": false,
    "blnIsFiler": true,
    "blnIsGst": true,
    "txtNtnNo": "1234567-8",
    "txtGstNumber": "GST-123456",
    "numDiscount": 5.00,
    "numSalesTax": 17.00,
    "dteCreateddate": "2024-01-15T10:30:00",
    "dteModifieddate": "2024-01-20T14:20:00",
    "cfgTblCity": {
      "serCityId": 1,
      "txtCityName": "Karachi"
    },
    "cfgTblCustomerCategory": {
      "serCustomerCategoryId": 2,
      "txtCustomerCategoryName": "Premium Dealer",
      "txtCustomerCategoryCode": "ZC04"
    },
    "cfgTblCountry": {
      "serCountryId": 1,
      "txtCountryName": "Pakistan"
    },
    "hrTblEmployee": {
      "serEmployeeId": 10,
      "txtEmployeeName": "John Doe"
    }
  }
]
```

---

## 🔑 KEY PERFORMANCE INDICATORS (KPIs)

### 1. **Dealer Base KPIs**
- **Total Active Dealers**: Count where `blnStatus = true` AND `blIsDeleted = false`
- **Total Inactive Dealers**: Count where `blnStatus = false` AND `blIsDeleted = false`
- **Dealer Growth Rate**: (New dealers this month / Total dealers) × 100
- **Dealer Retention Rate**: (Active dealers / Total dealers) × 100
- **New Dealers This Month**: Count where `dteCreateddate` is within current month
- **Dealers by Category**: Group by `cfgTblCustomerCategory` and count

### 2. **Geographic Distribution KPIs**
- **Dealers by City**: Group by `cfgTblCity` and count
- **Dealers by Province**: Group by `txtProvince` and count
- **Dealers by Region**: Group by `cfgTblRegion` and count
- **Dealers by Area**: Group by `cfgTblArea` and count
- **Dealers by Zone**: Group by `cfgTblZone1`, `cfgTblZone2`, `cfgTblZone3` and count
- **Geographic Coverage**: Number of unique cities/provinces with dealers

### 3. **Tax & Compliance KPIs**
- **GST Registered Dealers**: Count where `blnIsGst = true`
- **Tax Filer Dealers**: Count where `blnIsFiler = true`
- **Compliance Rate**: (GST registered + Tax filers) / Total dealers × 100
- **Dealers with Valid NTN**: Count where `txtNtnNo IS NOT NULL`
- **Dealers with Valid GST**: Count where `txtGstNumber IS NOT NULL`
- **Expired Registrations**: Count where `dteExpiryDate < CURRENT_DATE`

### 4. **Contact & Communication KPIs**
- **Dealers with Email**: Count where `txtEmailAddress IS NOT NULL`
- **Dealers with Mobile**: Count where `txtMobileNo IS NOT NULL`
- **Contact Completeness**: (Dealers with all contact info / Total dealers) × 100
- **Dealers by Contact Status**: Group by presence of email/mobile/phone

### 5. **Employee Assignment KPIs**
- **Dealers by Assigned Employee**: Group by `hrTblEmployee.serEmployeeId` and count
- **Unassigned Dealers**: Count where `hrTblEmployee IS NULL`
- **Average Dealers per Employee**: Total dealers / Number of employees with dealers
- **Top Employees by Dealer Count**: Employees with most assigned dealers

### 6. **Category & Classification KPIs**
- **Dealers by Category**: Group by `cfgTblCustomerCategory` and count
- **Premium Dealers**: Count where category code = 'ZC04' or similar
- **Commercial vs Passenger**: 
  - Commercial: Count where `blnCommercial = true`
  - Passenger: Count where `blnPassanger = true`
- **Export Dealers**: Count where `blnIsExport = true`
- **Group Dealers**: Count where `blIsGroup = true`

### 7. **SAP Integration KPIs**
- **SAP Synced Dealers**: Count where `blIsPOSTEDToSAP = true`
- **SAP Sync Rate**: (Synced dealers / Total dealers) × 100
- **SAP Sync Failures**: Count where `txtErrorMsgFromSap IS NOT NULL`
- **Pending SAP Sync**: Count where `blIsPOSTEDToSAP = false`

### 8. **Financial KPIs** (When Combined with Deal Data)
- **Average Discount Given**: Average of `numDiscount` across all dealers
- **Dealers by Discount Tier**: 
  - High (>= 10%): Count where `numDiscount >= 10`
  - Medium (5-9%): Count where `numDiscount BETWEEN 5 AND 9`
  - Low (< 5%): Count where `numDiscount < 5`
- **Tax Configuration Distribution**: 
  - Sales Tax: Average `numSalesTax`
  - FED: Average `numFED`
  - Excise Duty: Average `numExciseDuty`

### 9. **Hierarchical KPIs**
- **Parent Dealers**: Count where `cfgTblCustomer IS NULL` (top-level)
- **Child Dealers**: Count where `cfgTblCustomer IS NOT NULL` (sub-dealers)
- **Group Dealers**: Count where `cfgTblGroupCustomer IS NOT NULL`
- **Dealer Network Depth**: Maximum hierarchy level

### 10. **Time-Based KPIs**
- **Dealers Created This Month**: Count where `dteCreateddate` is within current month
- **Dealers Created This Year**: Count where `dteCreateddate` is within current year
- **Recently Modified**: Count where `dteModifieddate` is within last 7 days
- **Average Dealer Age**: Average of (CURRENT_DATE - `dteCreateddate`)

### 11. **Data Quality KPIs**
- **Complete Profiles**: Dealers with all required fields filled
- **Missing Contact Info**: Count where email OR mobile is NULL
- **Missing Tax Info**: Count where NTN OR GST is NULL
- **Data Completeness Score**: (Filled fields / Total fields) × 100

### 12. **Operational KPIs**
- **Active vs Inactive Ratio**: `blnStatus = true` / `blnStatus = false`
- **Dealer Distribution**: 
  - By Division: Group by `cfgTblDivision`
  - By IncoTerm: Group by `cfgTblIncoTerm`
- **Account Status**: Count where `blIsAccountExist = true` vs `false`

### 13. **Dealer Performance KPIs** (Requires Joining with `sls_tbl_deal`)
- **Revenue per Dealer**: Sum of `num_Total` from deals grouped by `ser_dealer_id`
- **Deal Count per Dealer**: Count of deals where `ser_dealer_id` matches dealer
- **Average Deal Value per Dealer**: Average `num_Total` per dealer
- **Top Performing Dealers**: Dealers with highest revenue or deal count
- **Dealer Conversion Rate**: (Dealers with deals / Total dealers) × 100
- **Active Dealers**: Dealers with at least one deal in last 30/60/90 days
- **Inactive Dealers**: Dealers with no deals in last 90 days
- **Dealer Growth**: Revenue growth month-over-month or year-over-year
- **Dealer Market Share**: (Dealer revenue / Total revenue) × 100
- **Average Discount Utilization**: Average discount used per dealer
- **Payment Collection Rate**: (`num_amount_received` / `num_Total`) × 100 per dealer
- **Outstanding Balance per Dealer**: Sum of `num_remaining_balance` per dealer
- **Dealer by Deal Status**: 
  - Approved deals per dealer
  - Completed deals per dealer
  - Pending deals per dealer
- **Dealer Performance Ranking**: Rank dealers by revenue, deal count, or growth

---

## 📊 Important Database Fields

### **Table: `cfg_tbl_customer`**

#### **Primary Key**
- `ser_customer_id` (PK) - Unique identifier

#### **Dealer Identification**
- `bl_is_dealer` (Boolean) - Dealer flag (must be TRUE)
- `txt_customer_code` - Dealer code
- `txt_customer_name` - Dealer name
- `txt_business_name` - Business name
- `txt_invoice_name` - Invoice name
- `txt_sap_no` - SAP reference

#### **Status Fields**
- `bl_is_deleted` - Deletion flag
- `bln_status` - Active/Inactive status
- `bl_is_group` - Group customer flag
- `bl_is_account_exist` - Account existence
- `bl_is_labsa` - LABSA flag

#### **Tax & Compliance**
- `bln_is_filer` - Tax filer status
- `bln_is_gst` - GST registration
- `txt_ntn_no` - NTN number
- `txt_gst_number` - GST number
- `txt_str` - Sales Tax Registration
- `txt_ftn` - FTN number
- `txt_is_filer` - Filer status text
- `num_discount` - Discount percentage
- `num_sales_tax` - Sales tax percentage
- `num_excise_duty` - Excise duty percentage
- `num_fed` - Federal Excise Duty
- `num_further_tax` - Further tax

#### **Contact Information**
- `txt_email_address` - Email
- `txt_mobile_no` - Mobile number
- `txt_phone_no` - Primary phone
- `txt_phone_no2` - Secondary phone
- `txt_cnic_no` - CNIC number

#### **Address Fields**
- `txt_billing_address` - Billing address
- `txt_shipping_address` - Shipping address
- `txt_display_address` - Display address
- `txt_province` - Province

#### **Geographic Foreign Keys**
- `ser_city_id` (FK) → `cfg_tbl_city`
- `ser_country_id` (FK) → `cfg_tbl_country`
- `ser_area_id` (FK) → `cfg_tbl_area`
- `ser_region_id` (FK) → `cfg_tbl_region`
- `ser_zone1_id` (FK) → `cfg_tbl_zone1`
- `ser_zone2_id` (FK) → `cfg_tbl_zone2`
- `ser_zone3_id` (FK) → `cfg_tbl_zone3`

#### **Category & Classification**
- `ser_customer_category_id` (FK) → `cfg_tbl_customer_category`
- `ser_division_id` (FK) → `cfg_tbl_division`
- `ser_inco_terms_id` (FK) → `cfg_tbl_inco_term`

#### **Hierarchical Relationships**
- `ser_parent_customer_id` (FK) → `cfg_tbl_customer` (self-reference)
- `ser_group_customer_id` (FK) → `cfg_tbl_customer` (group reference)

#### **Employee Assignment**
- `ser_employee_id` (FK) → `hr_tbl_employee`

#### **Date Fields**
- `dte_createddate` - Creation timestamp
- `dte_modifieddate` - Last modification timestamp
- `dte_dob` - Date of birth
- `dte_expirty_date` - Expiry date
- `txt_expirty_date` - Expiry date (text)

#### **HOD Information**
- `txt_designation` - Designation
- `txt_hod` - Head of Department name
- `txt_hod_mobile` - HOD mobile
- `txt_hod_landline` - HOD landline
- `txt_hod_email_address` - HOD email

#### **SAP Integration**
- `bl_is_posted_to_Sap` - SAP sync status
- `txt_xml_sent` - XML sent to SAP
- `txt_xml_receive` - XML received from SAP
- `txt_return_msg` - Return message
- `txt_error_msg_from_Sap` - Error message

#### **Other Fields**
- `txt_fname` - First name
- `txt_user_name` - Username
- `txt_division` - Division text
- `txt_machine_ip` - Machine IP
- `ser_created_user_id` - Created by user
- `ser_modified_user_id` - Modified by user
- `ser_group_id` - Group ID
- `bln_commercial` - Commercial flag
- `bln_passanger` - Passenger flag
- `bln_is_export` - Export flag

---

## 📈 Sample KPI Queries

### Total Active Dealers
```sql
SELECT COUNT(*) as total_active_dealers
FROM cfg_tbl_customer
WHERE bl_is_dealer = TRUE 
  AND bl_is_deleted = FALSE 
  AND bln_status = TRUE;
```

### Dealers by City
```sql
SELECT 
  c.txt_city_name,
  COUNT(d.ser_customer_id) as dealer_count
FROM cfg_tbl_customer d
JOIN cfg_tbl_city c ON d.ser_city_id = c.ser_city_id
WHERE d.bl_is_dealer = TRUE 
  AND d.bl_is_deleted = FALSE
GROUP BY c.ser_city_id, c.txt_city_name
ORDER BY dealer_count DESC;
```

### GST Compliance Rate
```sql
SELECT 
  COUNT(*) as total_dealers,
  SUM(CASE WHEN bln_is_gst = TRUE THEN 1 ELSE 0 END) as gst_registered,
  (SUM(CASE WHEN bln_is_gst = TRUE THEN 1 ELSE 0 END) / COUNT(*)) * 100 as compliance_rate
FROM cfg_tbl_customer
WHERE bl_is_dealer = TRUE 
  AND bl_is_deleted = FALSE;
```

### Dealers by Category
```sql
SELECT 
  cc.txt_customer_category_name,
  cc.txt_customer_category_code,
  COUNT(d.ser_customer_id) as dealer_count
FROM cfg_tbl_customer d
JOIN cfg_tbl_customer_category cc ON d.ser_customer_category_id = cc.ser_customer_category_id
WHERE d.bl_is_dealer = TRUE 
  AND d.bl_is_deleted = FALSE
GROUP BY cc.ser_customer_category_id, cc.txt_customer_category_name, cc.txt_customer_category_code
ORDER BY dealer_count DESC;
```

### New Dealers This Month
```sql
SELECT COUNT(*) as new_dealers_this_month
FROM cfg_tbl_customer
WHERE bl_is_dealer = TRUE 
  AND bl_is_deleted = FALSE
  AND YEAR(dte_createddate) = YEAR(CURRENT_DATE)
  AND MONTH(dte_createddate) = MONTH(CURRENT_DATE);
```

### Dealers by Assigned Employee
```sql
SELECT 
  e.txt_employee_name,
  COUNT(d.ser_customer_id) as dealer_count
FROM cfg_tbl_customer d
LEFT JOIN hr_tbl_employee e ON d.ser_employee_id = e.ser_employee_id
WHERE d.bl_is_dealer = TRUE 
  AND d.bl_is_deleted = FALSE
GROUP BY e.ser_employee_id, e.txt_employee_name
ORDER BY dealer_count DESC;
```

### SAP Sync Status
```sql
SELECT 
  COUNT(*) as total_dealers,
  SUM(CASE WHEN bl_is_posted_to_Sap = TRUE THEN 1 ELSE 0 END) as synced,
  SUM(CASE WHEN bl_is_posted_to_Sap = FALSE THEN 1 ELSE 0 END) as pending,
  SUM(CASE WHEN txt_error_msg_from_Sap IS NOT NULL THEN 1 ELSE 0 END) as failed
FROM cfg_tbl_customer
WHERE bl_is_dealer = TRUE 
  AND bl_is_deleted = FALSE;
```

### Dealers with Missing Contact Information
```sql
SELECT COUNT(*) as dealers_missing_contact
FROM cfg_tbl_customer
WHERE bl_is_dealer = TRUE 
  AND bl_is_deleted = FALSE
  AND (txt_email_address IS NULL OR txt_mobile_no IS NULL);
```

### Average Discount by Dealer Category
```sql
SELECT 
  cc.txt_customer_category_name,
  AVG(d.num_discount) as avg_discount,
  MIN(d.num_discount) as min_discount,
  MAX(d.num_discount) as max_discount
FROM cfg_tbl_customer d
JOIN cfg_tbl_customer_category cc ON d.ser_customer_category_id = cc.ser_customer_category_id
WHERE d.bl_is_dealer = TRUE 
  AND d.bl_is_deleted = FALSE
  AND d.num_discount IS NOT NULL
GROUP BY cc.ser_customer_category_id, cc.txt_customer_category_name;
```

### Top 10 Dealers by Revenue (Last 30 Days)
```sql
SELECT 
  c.ser_customer_id,
  c.txt_customer_name,
  c.txt_customer_code,
  SUM(d.num_Total) as total_revenue,
  COUNT(d.ser_deal_id) as deal_count,
  AVG(d.num_Total) as avg_deal_value
FROM cfg_tbl_customer c
JOIN sls_tbl_deal d ON c.ser_customer_id = d.ser_dealer_id
WHERE c.bl_is_dealer = TRUE 
  AND c.bl_is_deleted = FALSE
  AND d.bl_is_deleted = FALSE
  AND d.dte_createddate >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY c.ser_customer_id, c.txt_customer_name, c.txt_customer_code
ORDER BY total_revenue DESC
LIMIT 10;
```

### Dealer Performance Summary
```sql
SELECT 
  c.ser_customer_id,
  c.txt_customer_name,
  COUNT(d.ser_deal_id) as total_deals,
  SUM(d.num_Total) as total_revenue,
  SUM(d.num_amount_received) as amount_received,
  SUM(d.num_remaining_balance) as outstanding_balance,
  SUM(CASE WHEN d.bln_is_completed = TRUE THEN 1 ELSE 0 END) as completed_deals,
  SUM(CASE WHEN d.bln_is_approved = TRUE THEN 1 ELSE 0 END) as approved_deals,
  AVG(d.num_Total) as avg_deal_value
FROM cfg_tbl_customer c
LEFT JOIN sls_tbl_deal d ON c.ser_customer_id = d.ser_dealer_id 
  AND d.bl_is_deleted = FALSE
WHERE c.bl_is_dealer = TRUE 
  AND c.bl_is_deleted = FALSE
GROUP BY c.ser_customer_id, c.txt_customer_name
ORDER BY total_revenue DESC;
```

### Active vs Inactive Dealers (Based on Deal Activity)
```sql
SELECT 
  CASE 
    WHEN deal_count > 0 THEN 'Active'
    ELSE 'Inactive'
  END as dealer_status,
  COUNT(*) as dealer_count
FROM (
  SELECT 
    c.ser_customer_id,
    COUNT(d.ser_deal_id) as deal_count
  FROM cfg_tbl_customer c
  LEFT JOIN sls_tbl_deal d ON c.ser_customer_id = d.ser_dealer_id 
    AND d.bl_is_deleted = FALSE
    AND d.dte_createddate >= DATE_SUB(NOW(), INTERVAL 90 DAY)
  WHERE c.bl_is_dealer = TRUE 
    AND c.bl_is_deleted = FALSE
  GROUP BY c.ser_customer_id
) dealer_stats
GROUP BY dealer_status;
```

### Dealer Payment Collection Rate
```sql
SELECT 
  c.ser_customer_id,
  c.txt_customer_name,
  SUM(d.num_Total) as total_amount,
  SUM(d.num_amount_received) as amount_received,
  SUM(d.num_remaining_balance) as outstanding,
  (SUM(d.num_amount_received) / NULLIF(SUM(d.num_Total), 0)) * 100 as collection_rate
FROM cfg_tbl_customer c
JOIN sls_tbl_deal d ON c.ser_customer_id = d.ser_dealer_id
WHERE c.bl_is_dealer = TRUE 
  AND c.bl_is_deleted = FALSE
  AND d.bl_is_deleted = FALSE
  AND d.num_Total > 0
GROUP BY c.ser_customer_id, c.txt_customer_name
HAVING total_amount > 0
ORDER BY collection_rate DESC;
```

---

## 💡 Recommendations

1. **Dealer Performance Dashboard**: Combine dealer data with deal/sale order data to show:
   - Revenue per dealer
   - Deal count per dealer
   - Average deal value per dealer
   - Growth trends

2. **Geographic Analysis**: 
   - Map dealers by location
   - Identify coverage gaps
   - Plan expansion strategies

3. **Compliance Monitoring**:
   - Track GST registration status
   - Monitor expiry dates
   - Ensure tax information completeness

4. **Contact Management**:
   - Identify dealers with missing contact info
   - Regular data quality audits
   - Update contact information regularly

5. **Employee Performance**:
   - Track dealers per sales employee
   - Identify top-performing employees
   - Balance dealer assignments

6. **SAP Integration Health**:
   - Monitor sync status
   - Track sync failures
   - Ensure data consistency

7. **Category Analysis**:
   - Analyze dealer distribution by category
   - Identify high-value dealer categories
   - Plan category-based strategies

---

## 🔗 Related Endpoints

- `/getActiveDealer` - Returns only active dealers (`blnStatus = true`)
- `/getAllCustomer` - Returns all customers (dealers + regular customers)
- `/getActiveCustomer` - Returns active customers
- `/getCustomerWODealer` - Returns customers that are not dealers
- `/getgroupActiveCustomer` - Returns active group customers

---

## ⚠️ Important Notes

1. **Lazy Loading**: The `slsTblSaleOrders` relationship is lazy-loaded and may not be included in the response by default. To get deal information, use the `/searchDeal` endpoint with dealer filters.

2. **Performance**: For large dealer lists, consider implementing pagination or filtering.

3. **Data Relationships**: To calculate dealer performance KPIs (revenue, deal count), you'll need to join with the `sls_tbl_deal` table using `ser_dealer_id`.

4. **Security**: The endpoint respects user group filtering (commented out in current implementation but may be enabled).

5. **Data Completeness**: Some fields may be null. Always check for null values when calculating KPIs.

