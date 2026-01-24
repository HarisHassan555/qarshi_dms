# Enhanced `/searchDeal` Endpoint Documentation

## Overview
The `/searchDeal` endpoint has been enhanced to support comprehensive query-based searching with support for statuses, flags, dates, and all important fields from the `sls_tbl_deal` table.

## Endpoint Details
- **URL**: `/VIM/searchDeal`
- **Method**: `POST`
- **Content-Type**: `application/json`
- **Response**: `List<SlsTblDeal>`

## Request Body Structure

The endpoint accepts a JSON object with the following searchable fields:

### Basic Search Fields
```json
{
  "txtDealNo": "DEAL-001",           // Deal number (partial match)
  "txtSapNo": "SAP123",              // SAP number (partial match)
  "txtPONo": "PO-456",               // Purchase order number (partial match)
  "txtDCNo": "DC-789",                // Delivery Challan number (partial match)
  "txtInvoiceNo": "INV-012",          // Invoice number (partial match)
  "txtDealer": "Dealer Name",         // Dealer name (min 3 chars, partial match)
  "txtCustomer": "Customer Name",     // Customer name (min 3 chars, partial match)
  "txtProduct": "Product Name",       // Product name (min 3 chars, partial match)
  "serDealId": 123                    // Exact deal ID match
}
```

### Status Fields
```json
{
  "txtStatus": "APPROVED",            // Deal status (exact match)
  "txtReceiveStatus": "RECEIVED",      // Receipt status (exact match)
  "txtDCStatus": "GENERATED",          // Delivery Challan status (exact match)
  "txtInvoiceStatus": "INVOICED"       // Invoice status (exact match)
}
```

### Boolean Flags
```json
{
  "blIsDeleted": false,                // Include/exclude deleted records (default: false)
  "blnIsApproved": true,               // Approval status
  "blnIsCompleted": true,             // Completion status
  "blnDealCompletionStatus": true,     // Deal completion status
  "blnFromSAP": true,                 // SAP sync status
  "blnIsIncoTerm": true,               // Incoterm flag
  "blIsComplementry": true,           // Complementary deal flag
  "blIsSplit": false                   // Split deal flag
}
```

### Date Range Searches
```json
{
  "dte_date_from": "01-01-2024",      // Creation date from (dd-MM-yyyy)
  "dte_date_to": "31-12-2024",        // Creation date to (dd-MM-yyyy)
  "dteDueDate": "2024-12-31",         // Due date (Date object)
  "dteStartDate": "2024-01-01",        // Start date (Date object)
  "dteEndDate": "2024-12-31"           // End date (Date object)
}
```

### Relationship-Based Searches
```json
{
  "cfgTblProduct": {
    "serProductId": 10                 // Product ID
  },
  "cfgTblCustomer": {
    "serCustomerId": 20                // Customer ID
  },
  "cfgTblDealer": {
    "serCustomerId": 30                // Dealer ID (customer ID reference)
  },
  "cfgTblCity": {
    "serCityId": 5                     // City ID
  },
  "hrTblEmployee": {
    "serEmployeeId": 15                // Employee ID
  },
  "serCreatedUserId": 100,             // Created by user ID
  "serApprovedbyId": 101               // Approved by user ID
}
```

## Example Requests

### Example 1: Search by Status and Approval
```json
{
  "txtStatus": "APPROVED",
  "blnIsApproved": true,
  "blnIsCompleted": false,
  "dte_date_from": "01-01-2024",
  "dte_date_to": "31-12-2024"
}
```

### Example 2: Search Pending Approvals
```json
{
  "blnIsApproved": false,
  "blIsDeleted": false,
  "dte_date_from": "01-01-2024"
}
```

### Example 3: Search Overdue Deals
```json
{
  "blnIsCompleted": false,
  "blIsDeleted": false,
  "dteDueDate": "2024-01-01"  // Use current date or past date
}
```

### Example 4: Search by Customer and Product
```json
{
  "cfgTblCustomer": {
    "serCustomerId": 20
  },
  "cfgTblProduct": {
    "serProductId": 10
  },
  "txtStatus": "APPROVED"
}
```

### Example 5: Search SAP Synced Deals
```json
{
  "blnFromSAP": true,
  "dte_date_from": "01-01-2024",
  "dte_date_to": "31-12-2024"
}
```

### Example 6: Search by Invoice Status
```json
{
  "txtInvoiceStatus": "PENDING",
  "txtDCStatus": "GENERATED",
  "blIsDeleted": false
}
```

### Example 7: Search by Employee
```json
{
  "hrTblEmployee": {
    "serEmployeeId": 15
  },
  "dte_date_from": "01-01-2024",
  "dte_date_to": "31-12-2024"
}
```

### Example 8: Complex Search - Multiple Criteria
```json
{
  "txtStatus": "APPROVED",
  "blnIsCompleted": false,
  "blnIsApproved": true,
  "txtReceiveStatus": "PENDING",
  "cfgTblProduct": {
    "serProductId": 10
  },
  "dte_date_from": "01-01-2024",
  "dte_date_to": "31-12-2024",
  "serCreatedUserId": 100
}
```

## Response Format

The endpoint returns a list of `SlsTblDeal` objects with all associated fields populated:

```json
[
  {
    "serDealId": 123,
    "txtDealNo": "DEAL-001",
    "txtDealName": "Deal Name",
    "txtStatus": "APPROVED",
    "blnIsApproved": true,
    "blnIsCompleted": false,
    "dteCreateddate": "2024-01-15T10:30:00",
    "dteDueDate": "2024-02-15",
    "numTotal": 50000.00,
    "cfgTblCustomer": { ... },
    "cfgTblProduct": { ... },
    ...
  }
]
```

## Important Notes

1. **Date Format**: 
   - Creation date range: Use `dd-MM-yyyy` format for `dte_date_from` and `dte_date_to`
   - Other dates: Use standard Date format

2. **Partial Matching**: 
   - Text fields like `txtDealNo`, `txtSapNo`, `txtDealer`, `txtCustomer`, `txtProduct` support partial matching (LIKE query)
   - Minimum 3 characters required for dealer, customer, and product searches

3. **Default Behavior**:
   - Deleted records (`blIsDeleted = true`) are excluded by default
   - Results are ordered by `serDealId DESC` (newest first)
   - User's group ID is automatically applied as a filter

4. **Security**:
   - The endpoint uses parameterized queries to prevent SQL injection
   - User-based filtering is automatically applied based on logged-in user's customer/dealer relationship

5. **Performance**:
   - Use specific ID-based searches when possible for better performance
   - Date ranges should be reasonable (avoid very large ranges)

## Common Use Cases

### Find All Pending Approvals
```json
{
  "blnIsApproved": false,
  "txtStatus": "PENDING"
}
```

### Find Completed Deals This Month
```json
{
  "blnIsCompleted": true,
  "dte_date_from": "01-12-2024",
  "dte_date_to": "31-12-2024"
}
```

### Find Deals by Specific Customer
```json
{
  "cfgTblCustomer": {
    "serCustomerId": 20
  }
}
```

### Find Overdue Deals
```json
{
  "blnIsCompleted": false,
  "dteDueDate": "2024-01-01"  // Current date or earlier
}
```

### Find SAP Sync Issues
```json
{
  "blnFromSAP": false,
  "txtSoapReturnType": "ERROR"
}
```

## Error Handling

- Invalid date formats are logged but don't break the query
- Missing or null fields are ignored (not added to query)
- Empty strings are treated as null
- The endpoint returns an empty list if no matches are found

## Frontend Integration Example

```typescript
// Angular/TypeScript example
searchDeals(searchCriteria: any) {
  const payload = {
    txtStatus: searchCriteria.status,
    blnIsApproved: searchCriteria.isApproved,
    dte_date_from: this.formatDate(searchCriteria.fromDate),
    dte_date_to: this.formatDate(searchCriteria.toDate),
    cfgTblProduct: searchCriteria.productId ? { serProductId: searchCriteria.productId } : null
  };
  
  return this.http.post<SlsTblDeal[]>(`${this.apiUrl}/searchDeal`, payload);
}
```







