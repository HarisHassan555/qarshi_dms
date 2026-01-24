# Deal Management - KPIs and Important Database Fields

## Endpoint: `/searchDeal`
**Method:** POST  
**Controller:** `DealController.searchDealAction()`  
**Service:** `DealService.searchDeal()`  
**Returns:** `List<SlsTblDeal>`

## Database Table: `sls_tbl_deal`

---

## 🔑 KEY PERFORMANCE INDICATORS (KPIs)

### 1. **Financial KPIs**
- **Total Revenue**: Sum of `num_Total` or `num_gross_value`
- **Net Revenue**: Sum of `num_net_amount`
- **Average Deal Value**: Average of `num_Total`
- **Total Discount Given**: Sum of `num_discount_amount`
- **Total Tax Collected**: Sum of (`num_sales_tax_amount` + `num_fed_amount` + `num_cvt_amount` + `num_tof_amount` + `num_advance_tax`)
- **Revenue by Product**: Group by `ser_product_id` and sum `num_Total`
- **Revenue by Customer**: Group by `ser_customer_id` and sum `num_Total`
- **Revenue by Dealer**: Group by `ser_dealer_id` and sum `num_Total`

### 2. **Sales Volume KPIs**
- **Total Quantity Sold**: Sum of `num_quantity`
- **Number of Deals**: Count of records
- **Active Deals**: Count where `bl_is_deleted = false`
- **Completed Deals**: Count where `bln_is_completed = true`
- **Deals by Status**: Group by `txt_status`

### 3. **Operational KPIs**
- **Deal Completion Rate**: (`bln_is_completed = true` / Total deals) × 100
- **Approval Rate**: (`bln_is_approved = true` / Total deals) × 100
- **Average Deal Processing Time**: Average of (`dte_modifieddate` - `dte_createddate`)
- **On-Time Delivery Rate**: Count where `dte_due_date >= actual_delivery_date` / Total deals
- **Pending Approvals**: Count where `bln_is_approved = false` AND `txt_status = 'PENDING'`

### 4. **Time-Based KPIs**
- **Deals Created Today/This Week/This Month**: Filter by `dte_createddate`
- **Deals Due Soon**: Count where `dte_due_date` is within next 7 days
- **Overdue Deals**: Count where `dte_due_date < current_date` AND `bln_is_completed = false`
- **Average Time to Approval**: Average of (`dte_final_approval` - `dte_createddate`)
- **RSM Approval Time**: Average of (`dte_rsm_approval` - `dte_createddate`)

### 5. **Customer/Dealer KPIs**
- **Top Customers by Revenue**: Group by `ser_customer_id`, sum `num_Total`, order DESC
- **Top Dealers by Volume**: Group by `ser_dealer_id`, count deals
- **Customer Retention**: Count unique `ser_customer_id` with multiple deals
- **Dealer Performance**: Revenue and volume by `ser_dealer_id`

### 6. **Product KPIs**
- **Best Selling Products**: Group by `ser_product_id`, sum `num_quantity`
- **Product Revenue**: Group by `ser_product_id`, sum `num_Total`
- **Product Mix**: Distribution of deals across products

### 7. **SAP Integration KPIs**
- **SAP Sync Rate**: (`bln_from_SAP = true` / Total deals) × 100
- **SAP Sync Failures**: Count where `txtSoapReturnType != 'SUCCESS'`
- **Deals Pending SAP Sync**: Count where `txt_sap_no IS NULL` AND `bln_from_SAP = false`

### 8. **Payment & Delivery KPIs**
- **Payment Collection Rate**: (`num_amount_received` / `num_Total`) × 100
- **Outstanding Balance**: Sum of (`num_Total` - `num_amount_received`)
- **DC Generation Rate**: Count where `txt_dc_no IS NOT NULL` / Total deals
- **Invoice Generation Rate**: Count where `txt_invoice_no IS NOT NULL` / Total deals
- **Delivery Status**: Group by `txt_receive_status`

### 9. **Geographic KPIs**
- **Revenue by City**: Group by `ser_city_id`, sum `num_Total`
- **Deals by Distribution Channel**: Group by `ser_distribution_channel_id`
- **Deals by Sales Organization**: Group by `ser_sales_organization_id`
- **Deals by Division**: Group by `ser_division_id`

### 10. **Employee Performance KPIs**
- **Deals by Employee**: Group by `ser_Employee_id`, count and sum revenue
- **Top Performers**: Employees with highest revenue or deal count
- **Deals Created by User**: Group by `ser_created_user_id`

---

## 📊 IMPORTANT DATABASE FIELDS

### **Primary Key & Identifiers**
- `ser_deal_id` (PK) - Unique deal identifier
- `txt_deal_no` - Deal number (searchable)
- `txt_deal_name` - Deal name/description
- `txt_sap_no` - SAP system reference number

### **Status & Flags**
- `bl_is_deleted` - Soft delete flag
- `bln_deal_status` - Deal completion status
- `bln_is_approved` - Approval status
- `bln_is_completed` - Completion status
- `txt_status` - Deal status (e.g., 'APPROVED', 'PENDING', 'REJECTED')
- `txt_receive_status` - Delivery/receipt status
- `txt_dc_status` - Delivery Challan status
- `txt_invoice_status` - Invoice status
- `bln_from_SAP` - Indicates if deal synced from SAP
- `bl_is_complementry` - Complementary deal flag
- `bl_is_split` - Split deal flag
- `bln_is_incoterm` - Incoterm flag
- `bl_is_gal` - GAL flag

### **Financial Fields**
- `num_quantity` - Quantity ordered
- `num_price` - Unit price
- `num_total_price` - Total price (quantity × price)
- `num_net_amount` - Net amount
- `num_discount` - Discount percentage
- `num_discount_amount` - Discount amount
- `num_amount_after_discount` - Amount after discount
- `num_fex` / `num_fed` - FED (Federal Excise Duty)
- `num_fed_amount` - FED amount
- `num_amount_after_fed` - Amount after FED
- `num_sales_Tax` - Sales tax percentage
- `num_sales_tax_amount` - Sales tax amount
- `num_amount_after_st` - Amount after sales tax
- `num_cvt` - CVT percentage
- `num_cvt_amount` - CVT amount
- `num_amount_after_cvt` - Amount after CVT
- `num_freight` - Freight charges
- `num_tax_on_freight` - Tax on freight
- `num_tof_amount` - Tax on freight amount
- `num_gross_value` - Gross value
- `num_advance_tax` - Advance tax
- `num_Total` - Final total amount
- `num_amount` - Amount field
- `num_excise_duty` - Excise duty
- `num_amount_received` - Amount received (payment)
- `num_remaining_balance` - Outstanding balance

### **Date & Time Fields**
- `dte_createddate` - Deal creation timestamp
- `dte_modifieddate` - Last modification timestamp
- `dte_date` - Deal date
- `dte_start_date` - Start date
- `dte_end_date` - End date
- `dte_due_date` - Due date
- `dte_rsm_approval` - RSM approval timestamp
- `dte_final_approval` - Final approval timestamp
- `dte_po_date` - Purchase order date
- `dte_issue_date` - Issue date
- `txt_dc_date` - Delivery Challan date (string)
- `txt_invoice_Date` - Invoice date (string)
- `txt_order_approval_date` - Order approval date (string)

### **Customer & Dealer Information**
- `ser_customer_id` (FK) - Customer reference → `cfg_tbl_customer`
- `ser_dealer_id` (FK) - Dealer reference → `cfg_tbl_customer`
- `txt_customer` - Customer name (text)
- `txt_dealer` - Dealer name (text)

### **Product Information**
- `ser_product_id` (FK) - Product reference → `cfg_tbl_product`
- `txt_product` - Product name (text)

### **Employee & User Information**
- `ser_created_user_id` - User who created the deal
- `ser_modified_user_id` - User who last modified
- `ser_preparedby_id` - User who prepared the deal
- `ser_approvedby_id` - User who approved
- `ser_Employee_id` (FK) - Employee reference → `hr_tbl_employee`
- `ser_group_id` - Group/team identifier

### **Geographic & Organization Fields**
- `ser_city_id` (FK) - City reference → `cfg_tbl_city`
- `ser_distribution_channel_id` (FK) - Distribution channel
- `ser_division_id` (FK) - Division
- `ser_document_type_id` (FK) - Document type
- `ser_inco_terms_id` (FK) - Incoterms
- `ser_payment_terms_id` (FK) - Payment terms
- `ser_sales_organization_id` (FK) - Sales organization

### **Address & Shipping Fields**
- `txt_billing_address` - Billing address
- `txt_shipping_address1` - Shipping address line 1
- `txt_shipping_address2` - Shipping address line 2
- `txt_shipping_address3` - Shipping address line 3
- `txt_destination` - Destination
- `txt_ship_by` - Shipping method
- `txt_delivery_time` - Delivery time
- `txt_vehicle_type` - Vehicle type for delivery

### **Document Numbers**
- `txt_po_no` - Purchase order number
- `txt_dc_no` - Delivery Challan number
- `txt_invoice_no` - Invoice number
- `txt_issue_code` - Issue code

### **Payment & Terms**
- `txt_paymnet_terms` - Payment terms (text)
- `txt_price_terms` - Price terms

### **SAP Integration Fields**
- `txtSoapReturnType` - SAP SOAP response type
- `txtSoapResponseMsg` - SAP SOAP response message
- `num_sales_Tax_Sap` - Sales tax from SAP (string)

### **Other Fields**
- `txt_description` - Deal description
- `txt_machine_ip` - Machine IP address
- `priority` - Deal priority
- `txtType` - Deal type
- `txt_image` - Image data (BLOB)
- `profile_pic` - Profile picture (BLOB)
- `txt_image_name` - Image file name
- `txt_image_type` - Image file type
- `txt_dc_qty` - Delivery Challan quantity (string)

---

## 🔍 Search/Filter Capabilities

The `/searchDeal` endpoint supports filtering by:
- Deal number (`txtDealNo`)
- Dealer name (`txtDealer`)
- Customer name (`txtCustomer`)
- Product name (`txtProduct`)
- SAP number (`txtSapNo`)
- Product ID (`serProductId`)
- Deal ID (`serDealId`)
- Date range (`dte_date_from`, `dte_date_to`)
- Status (`txtStatus`)
- Complementary deals (`blIsComplementry`)
- Group ID (automatically filtered by user's group)

---

## 📈 Sample KPI Queries

### Total Revenue (Last 30 Days)
```sql
SELECT SUM(num_Total) as total_revenue
FROM sls_tbl_deal
WHERE dte_createddate >= DATE_SUB(NOW(), INTERVAL 30 DAY)
  AND bl_is_deleted = false;
```

### Deal Completion Rate
```sql
SELECT 
  COUNT(*) as total_deals,
  SUM(CASE WHEN bln_is_completed = true THEN 1 ELSE 0 END) as completed_deals,
  (SUM(CASE WHEN bln_is_completed = true THEN 1 ELSE 0 END) / COUNT(*)) * 100 as completion_rate
FROM sls_tbl_deal
WHERE bl_is_deleted = false;
```

### Top 10 Customers by Revenue
```sql
SELECT 
  c.txt_customer_name,
  SUM(d.num_Total) as total_revenue,
  COUNT(d.ser_deal_id) as deal_count
FROM sls_tbl_deal d
JOIN cfg_tbl_customer c ON d.ser_customer_id = c.ser_customer_id
WHERE d.bl_is_deleted = false
GROUP BY d.ser_customer_id, c.txt_customer_name
ORDER BY total_revenue DESC
LIMIT 10;
```

### Overdue Deals
```sql
SELECT COUNT(*) as overdue_deals
FROM sls_tbl_deal
WHERE dte_due_date < CURDATE()
  AND bln_is_completed = false
  AND bl_is_deleted = false;
```

---

## 💡 Recommendations

1. **Create a Dashboard** with these KPIs for real-time monitoring
2. **Set up Alerts** for overdue deals and pending approvals
3. **Generate Reports** weekly/monthly with trend analysis
4. **Track Conversion Funnel**: Created → Approved → Completed → Invoiced
5. **Monitor SAP Sync** to ensure data consistency
6. **Analyze Payment Collection** to improve cash flow
7. **Track Employee Performance** for sales team management







