# Top 5 Media Houses Calculation for Sale Invoice

## Overview
This document explains how to calculate the **Top 5 Media Houses** from the `/sale-invoice` endpoint data, sorted by:
1. **Revenue** (total invoice amount)
2. **Total Number of Entries** (count of invoices)

## Data Structure

### Entity Relationships
- **Sale Invoice** (`sls_tbl_sale_order`) 
  - Has relationship: `slsTblDeal` → `SlsTblDeal`
  - Has relationship: `cfgTblCustomer` → `CfgTblCustomer` (direct customer)
- **Deal** (`sls_tbl_deal`)
  - Has relationship: `cfgTblDealer` → `CfgTblCustomer` (Media House/Dealer)
- **Media House** = `cfgTblDealer.txtCustomerName` (from the Deal entity)

### Key Fields
- **Invoice Amount**: `sls_tbl_sale_order.num_amount` (BigDecimal)
- **Media House Name**: `sls_tbl_deal.cfg_tbl_dealer.txt_customer_name` (String)
- **Invoice ID**: `sls_tbl_sale_order.ser_sale_order_id` (Integer)

## Database View
The sale invoice data is accessed through the view: `view_sales_invoice_list`

## Calculation Methods

### Method 1: Top 5 Media Houses by Revenue

#### SQL Query Approach
```sql
SELECT 
    d.cfg_tbl_dealer.txt_customer_name AS media_house_name,
    COALESCE(SUM(so.num_amount), 0) AS total_revenue,
    COUNT(so.ser_sale_order_id) AS invoice_count
FROM 
    sls_tbl_sale_order so
    INNER JOIN sls_tbl_deal d ON so.ser_deal_id = d.ser_deal_id
    INNER JOIN cfg_tbl_customer dealer ON d.ser_dealer_id = dealer.ser_customer_id
WHERE 
    so.bl_is_deleted = false 
    AND so.num_amount IS NOT NULL
    -- Add date filters if needed:
    -- AND so.dte_date >= '2024-01-01'
    -- AND so.dte_date <= '2024-12-31'
GROUP BY 
    dealer.ser_customer_id,
    dealer.txt_customer_name
ORDER BY 
    total_revenue DESC
LIMIT 5;
```

#### Alternative: Using the View
```sql
SELECT 
    media_house_name,  -- Assuming this column exists in view_sales_invoice_list
    SUM(invoice_amount) AS total_revenue,
    COUNT(*) AS invoice_count
FROM 
    view_sales_invoice_list
WHERE 
    -- Add filters as needed
GROUP BY 
    media_house_name
ORDER BY 
    total_revenue DESC
LIMIT 5;
```

### Method 2: Top 5 Media Houses by Total Number of Entries

#### SQL Query Approach
```sql
SELECT 
    dealer.txt_customer_name AS media_house_name,
    COUNT(so.ser_sale_order_id) AS total_entries,
    COALESCE(SUM(so.num_amount), 0) AS total_revenue
FROM 
    sls_tbl_sale_order so
    INNER JOIN sls_tbl_deal d ON so.ser_deal_id = d.ser_deal_id
    INNER JOIN cfg_tbl_customer dealer ON d.ser_dealer_id = dealer.ser_customer_id
WHERE 
    so.bl_is_deleted = false
    -- Add date filters if needed:
    -- AND so.dte_date >= '2024-01-01'
    -- AND so.dte_date <= '2024-12-31'
GROUP BY 
    dealer.ser_customer_id,
    dealer.txt_customer_name
ORDER BY 
    total_entries DESC
LIMIT 5;
```

#### Alternative: Using the View
```sql
SELECT 
    media_house_name,
    COUNT(*) AS total_entries,
    SUM(invoice_amount) AS total_revenue
FROM 
    view_sales_invoice_list
WHERE 
    -- Add filters as needed
GROUP BY 
    media_house_name
ORDER BY 
    total_entries DESC
LIMIT 5;
```

## Java Implementation

### Option 1: Add to ReportsDAO

Add these methods to `ReportsDAO.java`:

```java
/**
 * Get Top 5 Media Houses by Revenue
 * @param dto ReportDTO with optional date filters
 * @return List of maps containing media house name, total revenue, and invoice count
 */
@Override
public List<Map<String, Object>> getTop5MediaHousesByRevenue(ReportDTO dto) {
    this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
    
    StringBuilder SQL = new StringBuilder();
    SQL.append("SELECT ");
    SQL.append("    dealer.txt_customer_name AS media_house_name, ");
    SQL.append("    COALESCE(SUM(so.num_amount), 0) AS total_revenue, ");
    SQL.append("    COUNT(so.ser_sale_order_id) AS invoice_count ");
    SQL.append("FROM ");
    SQL.append("    sls_tbl_sale_order so ");
    SQL.append("    INNER JOIN sls_tbl_deal d ON so.ser_deal_id = d.ser_deal_id ");
    SQL.append("    INNER JOIN cfg_tbl_customer dealer ON d.ser_dealer_id = dealer.ser_customer_id ");
    SQL.append("WHERE ");
    SQL.append("    so.bl_is_deleted = false ");
    SQL.append("    AND so.num_amount IS NOT NULL ");
    
    // Add date filters if provided
    if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
        try {
            SQL.append(" AND so.dte_date >= '")
               .append(DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())))
               .append("'");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
        try {
            SQL.append(" AND so.dte_date <= '")
               .append(DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())))
               .append("'");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    SQL.append(" GROUP BY ");
    SQL.append("    dealer.ser_customer_id, ");
    SQL.append("    dealer.txt_customer_name ");
    SQL.append(" ORDER BY ");
    SQL.append("    total_revenue DESC ");
    SQL.append(" LIMIT 5 ");
    
    System.out.println("Top 5 Media Houses by Revenue SQL: " + SQL.toString());
    return this.jdbcTemplateObject.queryForList(SQL.toString());
}

/**
 * Get Top 5 Media Houses by Total Number of Entries
 * @param dto ReportDTO with optional date filters
 * @return List of maps containing media house name, invoice count, and total revenue
 */
@Override
public List<Map<String, Object>> getTop5MediaHousesByEntries(ReportDTO dto) {
    this.jdbcTemplateObject = new JdbcTemplate(this.dataSource);
    
    StringBuilder SQL = new StringBuilder();
    SQL.append("SELECT ");
    SQL.append("    dealer.txt_customer_name AS media_house_name, ");
    SQL.append("    COUNT(so.ser_sale_order_id) AS total_entries, ");
    SQL.append("    COALESCE(SUM(so.num_amount), 0) AS total_revenue ");
    SQL.append("FROM ");
    SQL.append("    sls_tbl_sale_order so ");
    SQL.append("    INNER JOIN sls_tbl_deal d ON so.ser_deal_id = d.ser_deal_id ");
    SQL.append("    INNER JOIN cfg_tbl_customer dealer ON d.ser_dealer_id = dealer.ser_customer_id ");
    SQL.append("WHERE ");
    SQL.append("    so.bl_is_deleted = false ");
    
    // Add date filters if provided
    if (dto.getDte_date_from() != null && dto.getDte_date_from().trim().length() > 0) {
        try {
            SQL.append(" AND so.dte_date >= '")
               .append(DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_from())))
               .append("'");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    if (dto.getDte_date_to() != null && dto.getDte_date_to().trim().length() > 0) {
        try {
            SQL.append(" AND so.dte_date <= '")
               .append(DATE_FORMATDB.format(DATE_FORMAT.parse(dto.getDte_date_to())))
               .append("'");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    SQL.append(" GROUP BY ");
    SQL.append("    dealer.ser_customer_id, ");
    SQL.append("    dealer.txt_customer_name ");
    SQL.append(" ORDER BY ");
    SQL.append("    total_entries DESC ");
    SQL.append(" LIMIT 5 ");
    
    System.out.println("Top 5 Media Houses by Entries SQL: " + SQL.toString());
    return this.jdbcTemplateObject.queryForList(SQL.toString());
}
```

### Option 2: In-Memory Processing (If using existing endpoint)

If you're using the existing `/sale-invoice` endpoint that returns all invoices, you can process them in memory:

```java
/**
 * Process sale invoice list to get top 5 media houses by revenue
 */
public List<Map<String, Object>> getTop5MediaHousesByRevenueFromList(
    List<Map<String, Object>> saleInvoiceList) {
    
    // Group by media house and sum revenue
    Map<String, MediaHouseStats> mediaHouseMap = new HashMap<>();
    
    for (Map<String, Object> invoice : saleInvoiceList) {
        // Extract media house name (adjust field name based on actual structure)
        String mediaHouseName = extractMediaHouseName(invoice);
        BigDecimal amount = extractAmount(invoice);
        
        if (mediaHouseName != null) {
            mediaHouseMap.computeIfAbsent(mediaHouseName, k -> new MediaHouseStats())
                .addInvoice(amount);
        }
    }
    
    // Sort by revenue and get top 5
    return mediaHouseMap.entrySet().stream()
        .sorted((a, b) -> b.getValue().getTotalRevenue()
            .compareTo(a.getValue().getTotalRevenue()))
        .limit(5)
        .map(entry -> {
            Map<String, Object> result = new HashMap<>();
            result.put("media_house_name", entry.getKey());
            result.put("total_revenue", entry.getValue().getTotalRevenue());
            result.put("invoice_count", entry.getValue().getInvoiceCount());
            return result;
        })
        .collect(Collectors.toList());
}

/**
 * Process sale invoice list to get top 5 media houses by entry count
 */
public List<Map<String, Object>> getTop5MediaHousesByEntriesFromList(
    List<Map<String, Object>> saleInvoiceList) {
    
    // Group by media house and count entries
    Map<String, MediaHouseStats> mediaHouseMap = new HashMap<>();
    
    for (Map<String, Object> invoice : saleInvoiceList) {
        String mediaHouseName = extractMediaHouseName(invoice);
        BigDecimal amount = extractAmount(invoice);
        
        if (mediaHouseName != null) {
            mediaHouseMap.computeIfAbsent(mediaHouseName, k -> new MediaHouseStats())
                .addInvoice(amount);
        }
    }
    
    // Sort by entry count and get top 5
    return mediaHouseMap.entrySet().stream()
        .sorted((a, b) -> Integer.compare(
            b.getValue().getInvoiceCount(), 
            a.getValue().getInvoiceCount()))
        .limit(5)
        .map(entry -> {
            Map<String, Object> result = new HashMap<>();
            result.put("media_house_name", entry.getKey());
            result.put("total_entries", entry.getValue().getInvoiceCount());
            result.put("total_revenue", entry.getValue().getTotalRevenue());
            return result;
        })
        .collect(Collectors.toList());
}

// Helper class
private static class MediaHouseStats {
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private int invoiceCount = 0;
    
    public void addInvoice(BigDecimal amount) {
        if (amount != null) {
            totalRevenue = totalRevenue.add(amount);
        }
        invoiceCount++;
    }
    
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public int getInvoiceCount() { return invoiceCount; }
}
```

## API Endpoint Implementation

### Add to ReportController or SaleOrderController

```java
/**
 * Get Top 5 Media Houses by Revenue
 * Endpoint: GET /VIM/top5MediaHousesByRevenue
 * Query Parameters (optional):
 *   - dte_date_from: Start date (format: DD-MM-YYYY)
 *   - dte_date_to: End date (format: DD-MM-YYYY)
 */
@RequestMapping(value = "/top5MediaHousesByRevenue", method = RequestMethod.GET)
public ResponseEntity<List<Map<String, Object>>> getTop5MediaHousesByRevenue(
    @RequestParam(required = false) String dte_date_from,
    @RequestParam(required = false) String dte_date_to) {
    
    try {
        ReportDTO dto = new ReportDTO();
        dto.setDte_date_from(dte_date_from);
        dto.setDte_date_to(dte_date_to);
        
        List<Map<String, Object>> result = reportService.getTop5MediaHousesByRevenue(dto);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        logger.error("Error getting top 5 media houses by revenue", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

/**
 * Get Top 5 Media Houses by Total Entries
 * Endpoint: GET /VIM/top5MediaHousesByEntries
 * Query Parameters (optional):
 *   - dte_date_from: Start date (format: DD-MM-YYYY)
 *   - dte_date_to: End date (format: DD-MM-YYYY)
 */
@RequestMapping(value = "/top5MediaHousesByEntries", method = RequestMethod.GET)
public ResponseEntity<List<Map<String, Object>>> getTop5MediaHousesByEntries(
    @RequestParam(required = false) String dte_date_from,
    @RequestParam(required = false) String dte_date_to) {
    
    try {
        ReportDTO dto = new ReportDTO();
        dto.setDte_date_from(dte_date_from);
        dto.setDte_date_to(dte_date_to);
        
        List<Map<String, Object>> result = reportService.getTop5MediaHousesByEntries(dto);
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        logger.error("Error getting top 5 media houses by entries", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

## Response Format

### Top 5 by Revenue Response
```json
[
  {
    "media_house_name": "ABC Media House",
    "total_revenue": 1500000.50,
    "invoice_count": 45
  },
  {
    "media_house_name": "XYZ Broadcasting",
    "total_revenue": 1200000.75,
    "invoice_count": 38
  },
  {
    "media_house_name": "DEF Communications",
    "total_revenue": 980000.25,
    "invoice_count": 32
  },
  {
    "media_house_name": "GHI Media Group",
    "total_revenue": 875000.00,
    "invoice_count": 28
  },
  {
    "media_house_name": "JKL News Network",
    "total_revenue": 750000.50,
    "invoice_count": 25
  }
]
```

### Top 5 by Entries Response
```json
[
  {
    "media_house_name": "ABC Media House",
    "total_entries": 45,
    "total_revenue": 1500000.50
  },
  {
    "media_house_name": "XYZ Broadcasting",
    "total_entries": 38,
    "total_revenue": 1200000.75
  },
  {
    "media_house_name": "DEF Communications",
    "total_entries": 32,
    "total_revenue": 980000.25
  },
  {
    "media_house_name": "GHI Media Group",
    "total_entries": 28,
    "total_revenue": 875000.00
  },
  {
    "media_house_name": "JKL News Network",
    "total_entries": 25,
    "total_revenue": 750000.50
  }
]
```

## Important Notes

1. **Field Name Verification**: Before implementing, verify the exact column names in:
   - `view_sales_invoice_list` view
   - `sls_tbl_sale_order` table
   - `sls_tbl_deal` table
   - `cfg_tbl_customer` table (for dealer relationship)

2. **Null Handling**: Ensure proper handling of:
   - Null amounts (`num_amount IS NOT NULL` filter)
   - Deleted records (`bl_is_deleted = false` filter)
   - Missing relationships (use `LEFT JOIN` if needed)

3. **Date Filtering**: Both methods support optional date range filtering using:
   - `dte_date_from`: Start date
   - `dte_date_to`: End date

4. **Performance**: For large datasets, the SQL-based approach (Method 1) is more efficient than in-memory processing (Method 2).

5. **Dealer Relationship**: The media house is accessed through:
   - `SlsTblSaleOrder` → `slsTblDeal` → `cfgTblDealer` → `txtCustomerName`

## Testing

Test the queries directly in MySQL:
```sql
-- Test Top 5 by Revenue
SELECT 
    dealer.txt_customer_name AS media_house_name,
    COALESCE(SUM(so.num_amount), 0) AS total_revenue,
    COUNT(so.ser_sale_order_id) AS invoice_count
FROM 
    sls_tbl_sale_order so
    INNER JOIN sls_tbl_deal d ON so.ser_deal_id = d.ser_deal_id
    INNER JOIN cfg_tbl_customer dealer ON d.ser_dealer_id = dealer.ser_customer_id
WHERE 
    so.bl_is_deleted = false 
    AND so.num_amount IS NOT NULL
GROUP BY 
    dealer.ser_customer_id,
    dealer.txt_customer_name
ORDER BY 
    total_revenue DESC
LIMIT 5;
```

## Next Steps

1. Verify the exact column/field names in your database schema
2. Add the methods to `IReportDAO` interface
3. Add the methods to `IReportService` interface
4. Implement in `ReportService`
5. Add controller endpoints
6. Test with sample data
7. Update frontend to consume the new endpoints







