# API Environments Configuration

## Production (PRD) - Currently Active

### Main SOAP Endpoint
- **URL:** `http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/300/zkf_e_invoice_web_service/zkf_e_invoice_web_service`
- **Property:** `soap.endpoint.url`
- **Server IP:** `192.0.2.150`
- **System Number:** `300`

### SES Web Service Endpoint
- **URL:** `http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_ses_web_service/300/zkf_e_invoice_ses_web_service/zkf_e_invoice_ses_web_service`
- **Property:** `soap.endpoint.url.ses`
- **Server IP:** `192.0.2.150`
- **System Number:** `300`

### Remarks Web Service Endpoint
- **URL:** `http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_invoice_workflow_remarks/300/zkf_invoice_workflow_remarks/zkf_invoice_workflow_remarks`
- **Property:** `soap.endpoint.url.remarks`
- **Server IP:** `192.0.2.150`
- **System Number:** `300`

### Credentials
- **Username:** `SAP-TMX`
- **Password:** `SaP@ExD123`
- **Properties:** `soap.username`, `soap.password`

### Common Configuration
- **SOAP Action:** `urn:sap-com:document:sap:soap:functions:mc-style`
- **Port:** `8000`

---

## Staging - Commented Out (Old)

### Main SOAP Endpoint
- **URL:** `http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/110/zkf_e_invoice_web_service/zkf_e_invoice_web_service`
- **Property:** `soap.endpoint.url` (commented)
- **Server IP:** `192.0.2.151`
- **System Number:** `110`

### SES Web Service Endpoint
- **URL:** `http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_ses_web_service/110/zkf_e_invoice_ses_web_service/zkf_e_invoice_ses_web_service`
- **Property:** `soap.endpoint.url.ses` (commented)
- **Server IP:** `192.0.2.151`
- **System Number:** `110`

### Remarks Web Service Endpoint
- **URL:** `http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_invoice_workflow_remarks/110/zkf_invoice_workflow_remarks/zkf_invoice_workflow_remarks`
- **Property:** `soap.endpoint.url.remarks` (commented)
- **Server IP:** `192.0.2.151`
- **System Number:** `110`

### Credentials
- **Username:** `MUJAHID_ABAP`
- **Password:** `dev110`
- **Properties:** `soap.username`, `soap.password` (commented)

### Common Configuration
- **SOAP Action:** `urn:sap-com:document:sap:soap:functions:mc-style`
- **Port:** `8000`

---

## Summary of Differences

| Configuration | Production | Staging |
|--------------|------------|---------|
| Server IP | 192.0.2.150 | 192.0.2.151 |
| System Number | 300 | 110 |
| Username | SAP-TMX | MUJAHID_ABAP |
| Password | SaP@ExD123 | dev110 |
| Port | 8000 | 8000 |
| Status | Active | Commented Out |

---

## Additional Notes

- There are also commented-out QAS (Quality Assurance) endpoints on IP `192.0.2.153` with system number `300` and credentials `Mujahid_abap`/`qas300`
- All environments use the same SOAP action: `urn:sap-com:document:sap:soap:functions:mc-style`
- All environments use port `8000`

