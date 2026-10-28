# SAP Transaction Codes: Velocity DMS Integration

Repository: **qarshi_dms** (Velocity document management + legacy VIM modules in `VIM-BE`).  
Backend context path: **`/velocity`** (`application.properties`).

This document maps confirmed SAP interfaces in code to standard SAP ECC / S/4HANA transaction codes for build, test, and operations. **Custom Z RFCs and web services are project-specific**; only T-codes listed here are standard SAP.

---

## Phase 1 summary (code analysis)

Active SAP-related integrations in this codebase:

- **(A) Velocity DMS master data** — SAP HCM employee pad via `ZHCM_EMP_INFO_PAD_WS`
- **(B) Legacy VIM procurement / invoice** — SOAP services `ZKF_E_INVOICE_WEB_SERVICE`, `ZKF_E_INVOICE_SES_WEB_SERVICE`, `ZKF_INVOICE_WORKFLOW_REMARKS`

No OData, IDoc, or direct SAP JCo/RFC client libraries were found for Velocity DMS document workflow. Scheduled SAP cron jobs for invoice import are **disabled** in code; triggers are manual HTTP endpoints or UI actions.

| Integration code | Direction | Protocol | SAP module | Status in repo |
|------------------|-----------|----------|------------|----------------|
| SAP-HCM-EMP | SAP → App | SOAP over HTTP (RFC web service) | HCM / PA | Live — Velocity UI + APIs |
| VIM-INV-IMPORT | SAP → App | SOAP (`Erdat` filter) | MM / FI | Live — manual `/sendPost` |
| VIM-SES | App ↔ SAP | SOAP (PO line + qty) | MM (SES) | Live — sale order DAO |
| VIM-WF-REMARKS | SAP → App | SOAP (`Belnr`) | FI / Workflow | Live — remarks sync |
| VIM-SD-DEAL-POST | App → SAP (legacy) | HTTP form POST `ZSO_CREATE_SRV` | SD | Legacy — confirm use |

---

## 1. Lead

Velocity DMS and the embedded VIM backend call SAP through **SOAP RFC web services** at `/sap/bc/srt/rfc/sap/...` (and one legacy HTTP service). The application **never runs SAP GUI T-codes**. T-codes below are for SAP functional and basis teams to configure, verify master data, and troubleshoot the same objects the interfaces read or write.

---

## 2. Interface-to-T-code map

| App integration | RFC / web service (Z) | Build / test (SAP) | Publish / transport | Likely SAP source |
|-----------------|----------------------|--------------------|-------------------|-------------------|
| SAP-HCM-EMP | `ZHCM_EMP_INFO_PAD_WS` | SE37, SE80, PA20/PA30, PPOME | SE09/SE10, STMS, SOAMANAGER, SICF | PA0001, PA0002, PA0008, org assignment — **to be confirmed by SAP team** |
| VIM-INV-IMPORT | `ZKF_E_INVOICE_WEB_SERVICE` (`ZkfEInvoiceWebService`) | SE37, ME23N, ME2N, FB03 | SOAMANAGER, SICF, STMS | EKKO/EKPO, vendor — `SoapClientService.convertElementToSlsTblDeal` |
| VIM-SES | `ZKF_E_INVOICE_SES_WEB_SERVICE` | SE37, MIGO, ML81N, ME23N | SOAMANAGER, SICF | PO / SES — EBELN, EBELP in request |
| VIM-WF-REMARKS | `ZKF_INVOICE_WORKFLOW_REMARKS` | SE37, FB03, SWI1 | SOAMANAGER, SICF | Invoice workflow — Belnr, Stext, WiAagent |
| VIM-SD-DEAL-POST | `ZSO_CREATE_SRV` (non-SICF URL) | SE37, VA01, VA03 | SICF or ICM — **to be confirmed** | SD sales order — `SlsTblDealDAO` |

### Endpoint URL patterns (from config — no credentials)

| Property | Pattern (example) |
|----------|-------------------|
| `sap.hcm.soap.endpoint` | `http://<host>:8000/sap/bc/srt/rfc/sap/zhcm_emp_info_pad_ws/<client>/zhcm_emp_info_pad_ws/zhcm_emp_info_pad_ws` |
| `soap.endpoint.url` | `http://<host>:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/<client>/zkf_e_invoice_web_service/zkf_e_invoice_web_service` |
| `soap.endpoint.url.ses` | `.../zkf_e_invoice_ses_web_service/...` |
| `soap.endpoint.url.remarks` | `.../zkf_invoice_workflow_remarks/...` |

Client **300** appears in current PRD samples in `application.properties`. Hosts differ between HCM and FI in config — confirm per environment.

---

## 3. Build, publish, and transport (SOAP RFC)

1. **SE37** — Verify Z function module; test with same parameters as web service (`Erdat`, `EBELN`, etc.).
2. **SE80** — Maintain function group and custom includes for Z objects.
3. **SOAMANAGER** — Web service definition and binding for each Z* service.
4. **SICF** — Activate path under `/sap/bc/srt/rfc/sap/<service>/<client>/...` matching `application.properties`.
5. **SM59** — HTTP destination (optional; app calls SAP URL directly).
6. **SU01 / PFCG** — Technical user and roles for HTTP Basic auth (`sap.hcm.soap.*`, `soap.username` — use env vars, not committed secrets).
7. **SE09 / SE10** — Transport Z objects and SICF from dev to QA/PRD.
8. **STMS** — Import transport queue.
9. **SCC4** — Confirm client matches URL segment (e.g. 300).

---

## 4. Interface detail

### 4.1 SAP-HCM-EMP (`ZHCM_EMP_INFO_PAD_WS`)

| Item | Value |
|------|--------|
| **Direction** | SAP → App |
| **Protocol** | SOAP POST, empty body element `ZHCM_EMP_INFO_PAD_WS` |
| **Auth** | HTTP Basic (`sap.hcm.soap.username`) |
| **Timeouts** | Connect 60s (`sap.hcm.soap.connect-timeout-ms`), read 900s (`sap.hcm.soap.read-timeout-ms`) |
| **Triggers** | `POST /velocity/syncSapHcmEmployeesFromSap`; `POST /velocity/syncSapHcmEmployeesFromXml` (multipart file) |
| **UI** | Master Data → SAP Employees (`sap-hcm-employees`) |

| XML element (`item`) | DB column (`cfg_tbl_sap_hcm_employee`) | Verify in SAP | Notes |
|----------------------|----------------------------------------|---------------|-------|
| EMPLOYEE | `txt_employee_id` | PA20 / PA30 | Key; row skipped if empty |
| EMP_NAME | `txt_emp_name` | PA20 | |
| DEPARTMENT_NAME | `txt_department_name` | PPOME / PA20 | Upserts `hr_tbl_department` |
| DIVISION | `txt_division` | PPOME | Upserts `hr_tbl_division` |
| DESIGNATION | `txt_designation` | PA20 | |
| EMAIL | `txt_email` | PA20 | |
| PAY_SCALE | `txt_pay_scale` | PA20 / PA30 | Infotype — confirm |
| SUBAREA | `txt_subarea` | PPOME | Upserts `hr_tbl_subarea` |
| COMPANY_NAME | `txt_company_name` | PPOME | Upserts `hr_tbl_organization` |
| HIRING_DATE | `txt_hiring_date` | PA20 | Stored as string |
| CONFIRMATION_DATE | `txt_confirmation_date` | PA20 | |
| RETIREMENT_DATE | `txt_retirement_date` | PA20 | |
| SEPARATION_DATE | `txt_separation_date` | PA20 | |
| REHIRING_DATE | `txt_rehiring_date` | PA20 | |
| ANNUAL_INC_TYPE | `txt_annual_inc_type` | PA20 | |
| ACTIVE | `txt_active` | PA20 | |
| REPORT_TO | `txt_report_to` | PA20 | Manager personnel number |
| REPORT_TO_NAME | `txt_report_to_name` | PA20 | |
| PICTURE_BASE64 | `txt_picture_path`, `bl_has_picture` | PA20 | Files under `sap_hcm_employee_photos` on disk |
| (all fields) | `txt_record_json` | — | JSON without picture |
| — | `dte_last_synced_date` | — | Set on each sync |

**Side effect:** `SapHcmOrgStructureSyncService` upserts `hr_tbl_organization` → `hr_tbl_subarea` → `hr_tbl_division` → `hr_tbl_department` from `COMPANY_NAME`, `SUBAREA`, `DIVISION`, `DEPARTMENT_NAME`.

**User provisioning:** SAP Employees UI can create `cfg_tbl_user` with `createdFromSapHcm=true` (manual). **No SAP write-back** in code.

**APIs:** `GET /getAllSapHcmEmployees`, `GET /getSapHcmEmployeePhoto?employeeId=`

---

### 4.2 VIM-INV-IMPORT (`ZKF_E_INVOICE_WEB_SERVICE`)

| Item | Value |
|------|--------|
| **Direction** | SAP → App |
| **Request** | `Erdat` (yyyy-MM-dd) |
| **Trigger** | `POST /velocity/sendPost` with JSON `{"erDate":"yyyy-MM-dd"}` |
| **Property** | `soap.endpoint.url` |
| **Processing** | `SoapClientService.readSOAPResponse` → `parseSOAPResponse` → `convertElementToSlsTblDeal` → `sls_tbl_deal` (+ customer/product) |

| Response XML tag | App target | SAP T-code | Notes |
|------------------|------------|------------|-------|
| Ebeln | `SlsTblDeal.txtDealNo` | ME23N | PO number |
| Ebelp | Deal line item | ME23N | |
| Aedat | Deal dates | ME23N | |
| Lifnr | `CfgTblCustomer.txtCustomerCode` | XK03 / FK03 | Vendor |
| Vendor | Customer name | XK03 | |
| Telf1 | Phone | XK03 | |
| Ort01 | City | XK03 | |
| Street | Address | XK03 | |
| Email | Email | XK03 | |
| Srvpos / Service | Product / service | ML81N | Confirm with SAP |
| Menge | Quantity | ME23N | |
| Netwr | Net value | ME23N | |
| TaxCode | `numSalesTaxSap` | FTXP / ME23N | |

---

### 4.3 VIM-SES (`ZKF_E_INVOICE_SES_WEB_SERVICE`)

| Item | Value |
|------|--------|
| **Direction** | App → SAP (request); SAP → App (response) |
| **Request fields** | `DOC_DATE`, `EBELN`, `EBELP`, `POST_DATE`, `QUANTITY` |
| **Property** | `soap.endpoint.url.ses` |
| **Code** | `SlsTblSaleOrderDAO` → `SoapClientService.sendSOAPRequestSES` / `readSOAPResponseSES` |

| SOAP field | App usage | SAP T-code | Notes |
|------------|-----------|------------|-------|
| EBELN | Purchase order | ME23N | |
| EBELP | PO line | ME23N | |
| DOC_DATE / POST_DATE | Dates on order | MIGO / ML81N | Confirm with SAP |
| QUANTITY | SES quantity | ML81N | |

---

### 4.4 VIM-WF-REMARKS (`ZKF_INVOICE_WORKFLOW_REMARKS`)

| Item | Value |
|------|--------|
| **Direction** | SAP → App |
| **Request** | `Belnr` via `Ziwfr` |
| **Property** | `soap.endpoint.url.remarks` |
| **Processing** | `parseSOAPResponseRemarks` → updates `SlsTblSaleOrder` |

| Response field | App column | SAP T-code | Notes |
|----------------|------------|------------|-------|
| Belnr | `txtSapInvoiceNo` | FB03 | Accounting document |
| Stext | `txtTaxRemarks` / `txtAuditRemarks` | SWI1 / FB03 | Workflow text |
| WiAagent | Routing (e.g. "Sale Tax") | SWI1 | |
| WiCd / WiCt | `dteCreateddate` | SWI1 | Date + time combined |

---

### 4.5 VIM-SD-DEAL-POST (legacy)

`SlsTblDealDAO.addDealTOSAP` posts form data to `ServerConfiguration.ip_servre` + `/ZSO_CREATE_SRV`.  
`ServerConfiguration.integeration_required` defaults to **false**. Treat as **legacy** — confirm before UAT.

---

## 5. Security

| Task | T-code | Notes |
|------|--------|-------|
| Service user | SU01 | `soap.username`, `sap.hcm.soap.username` — env only in PRD |
| Roles | PFCG | RFC + SICF auth per Z FM |
| Authorization test | SU53 | After SOAP 401/403 |
| Auth trace | ST01 | HCM + MM/FI objects |
| User audit | SUIM | Integration users |

Authorization objects depend on Z FM implementation — **to be confirmed by SAP team** (typical: `S_RFC`, `S_SERVICE`, HCM read, `M_BEST_*`, `F_BKPF_*`).

---

## 6. Monitoring and troubleshooting

| Symptom | Likely cause | T-code / tool |
|---------|--------------|---------------|
| HTTP 401 | Wrong Basic credentials | SU01, SU53, check properties |
| HTTP 404 | SICF inactive or wrong URL/client | SICF, SOAMANAGER |
| HTTP 500 | ABAP error in Z FM | ST22, SM21 |
| Timeout (HCM 900s read) | Large extract | ST05, SM50, SM66 |
| SOAP fault | FM exception / bad `Erdat` or `Belnr` | SE37, SRT_MONI |
| Empty `item` list | No data for date | SE37, ME2N |
| Partial org structure | Missing company/subarea in row | PPOME, PA20 |
| SSL errors | HTTPS mismatch | STRUST, SMICM |

---

## 7. Future / proposed (from code comments only)

- `create_background_job_table.sql` mentions `SAP_HCM_EMPLOYEE_SYNC` / `SAP_HCM_EMPLOYEE_XML_SYNC` — **not** in `BackgroundJobTypes.java` (email jobs only).
- `ScheduledTask` `@Scheduled` invoice import is **commented out**; `so.job.cron.expression` / `dep.job.cron.expression` in properties — **no active `@Scheduled` reference found**.
- No SAP HCM write-back (`HR_INFOTYPE_OPERATION`, `BAPI_EMPLOYEE_*`). User create from SAP screen writes only to MySQL `cfg_tbl_user`.

---

## 8. UAT checklist

| Test case | Set up in SAP | Verify in SAP | Verify in app |
|-----------|---------------|---------------|---------------|
| HCM full sync | PA20: sample pernr with org/email | PA20 / PPOME | `POST syncSapHcmEmployeesFromSap`; row in `cfg_tbl_sap_hcm_employee` |
| HCM XML import | Sample XML from SE37 test | SE37 | `POST syncSapHcmEmployeesFromXml` |
| HCM photo | Employee with photo | PA20 | `GET getSapHcmEmployeePhoto?employeeId=` |
| Org hierarchy | Valid company/subarea | PPOME | `hr_tbl_organization` … `hr_tbl_department` |
| Invoice import | PO for `Erdat` | ME2N | `POST /sendPost`; `sls_tbl_deal` |
| SES posting | Open PO line | ME23N | SES SOAP log / sale order |
| Workflow remarks | Invoice in workflow | FB03 / SWI1 | Remarks on `sls_tbl_sale_order` |
| Auth negative | Lock integration user | SU01 | HTTP 401 on SOAP |

---

## 9. T-code quick reference (alphabetical)

| T-code | Name | Used for |
|--------|------|----------|
| FB03 | Display document | FI invoice / Belnr |
| ME23N | Display PO | EKKO/EKPO, import & SES |
| ME2N | PO list by date | Erdat import testing |
| MIGO | Goods movement | SES related |
| ML81N | Service entry sheet | SES |
| PA20 | HR master data display | Employee sync |
| PA30 | HR master data maintain | HCM test data |
| PPOME | Org structure | Company / subarea / division |
| SCC4 | Client maintenance | Client in URL (e.g. 300) |
| SE09 | Transport organizer | Release Z objects |
| SE10 | Transport requests | Import checklist |
| SE37 | Function builder | Test Z FMs |
| SE80 | Object navigator | Z development |
| SICF | HTTP service tree | RFC SOAP paths |
| SM21 | System log | ICM / gateway |
| SM37 | Background jobs | SAP-side batch (if used) |
| SM50 | Work processes | Performance |
| SM59 | RFC destinations | Optional middleware |
| SM66 | Global work process overview | Load |
| SMICM | ICM monitor | HTTP/SOAP |
| SOAMANAGER | SOA management | Web service bindings |
| SRT_MONI | Web service monitor | SOAP runtime |
| ST01 | Authorization trace | After SU53 |
| ST05 | SQL trace | Performance |
| ST22 | ABAP dump | HTTP 500 |
| STMS | Transport management | PRD import |
| SU01 | User maintenance | Integration users |
| SU53 | Authorization check | Missing auth |
| SUIM | User information | Audit |
| SWI1 | Workflow inbox | Remarks interface |
| VA01 | Create sales order | Legacy ZSO_CREATE_SRV |
| VA03 | Display sales order | Legacy SD |
| XK03 | Display vendor | Lifnr on deals |

---

## Open items for SAP team

- Confirm `ZHCM_EMP_INFO_PAD_WS` ABAP source (tables/infotypes) for each XML field.
- Confirm SAP client and host per environment (PRD samples: client 300; HCM vs FI hosts differ in config).
- Provide WSDL/XSD for all Z web services; confirm SOAPAction (`sap.hcm.soap.soap-action` is empty in config).
- Confirm whether VIM invoice/SES/remarks are in scope for Velocity go-live or HCM-only.
- Confirm authorization roles for integration users.
- Clarify legacy `ZSO_CREATE_SRV` and `integeration_required` flag.
- Implement or drop planned `SAP_HCM_EMPLOYEE_SYNC` background jobs.
- Remove plaintext SAP passwords from `application.properties`; use environment variables.

---

## Code references

| Topic | File(s) |
|-------|---------|
| HCM SOAP client | `VIM-BE/src/main/java/.../SapHcmSoapClient.java` |
| HCM import & field map | `VIM-BE/src/main/java/.../SapHcmEmployeeService.java` |
| HCM org upsert | `VIM-BE/src/main/java/.../SapHcmOrgStructureSyncService.java` |
| HCM REST API | `VIM-BE/src/main/java/.../SapHcmEmployeeController.java` |
| HCM DB schema | `VIM-BE/scripts/create_sap_hcm_employee_tables.sql` |
| Config endpoints | `VIM-BE/src/main/resources/application.properties` |
| VIM SOAP client | `VIM-BE/src/main/java/.../SoapClientService.java` |
| Manual SAP jobs | `VIM-BE/.../SoapController.java`, `ScheduledTask.java` |
| Environment doc | `VIM-BE/STAGING_AND_PRODUCTION_APIS.md`, `api-environments.md` |
| FE SAP Employees UI | `VIM-FE/src/app/components/sap-hcm-employees/` |
