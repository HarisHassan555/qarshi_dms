"""Generate SAP T-code integration document for Velocity DMS / VIM-BE."""
from docx import Document
from docx.shared import Pt, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from pathlib import Path

OUT = Path(__file__).resolve().parent / "SAP_Transaction_Codes_Velocity_DMS_Integration.docx"


def add_table(doc, headers, rows):
    t = doc.add_table(rows=1, cols=len(headers))
    t.style = "Table Grid"
    hdr = t.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = h
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            cells[i].text = str(val)
    doc.add_paragraph()


def main():
    doc = Document()
    title = doc.add_heading("SAP Transaction Codes: Velocity DMS Integration", 0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER

    doc.add_paragraph(
        "Repository: qarshi_dms (Velocity document management + legacy VIM modules in VIM-BE). "
        "Backend context path: /velocity (application.properties). "
        "This document maps confirmed SAP interfaces in code to standard SAP ECC / S/4HANA transaction codes for build, test, and operations. "
        "Custom Z RFCs and web services are project-specific; only T-codes listed here are standard SAP."
    )

    doc.add_heading("Phase 1 summary (code analysis)", level=1)
    doc.add_paragraph(
        "Active SAP-related integrations found in this codebase fall into two groups: "
        "(A) Velocity DMS master data — SAP HCM employee pad via ZHCM_EMP_INFO_PAD_WS; "
        "(B) Legacy VIM procurement / invoice — SOAP services ZKF_E_INVOICE_WEB_SERVICE, "
        "ZKF_E_INVOICE_SES_WEB_SERVICE, and ZKF_INVOICE_WORKFLOW_REMARKS. "
        "No OData, IDoc, or direct SAP JCo/RFC client libraries were found for Velocity DMS document workflow. "
        "Scheduled SAP cron jobs for invoice import are disabled in code; triggers are manual HTTP endpoints or UI actions."
    )
    add_table(
        doc,
        ["Integration code", "Direction", "Protocol", "SAP module", "Status in repo"],
        [
            ["SAP-HCM-EMP", "SAP → App", "SOAP over HTTP (RFC web service)", "HCM / PA", "Live — Velocity UI + APIs"],
            ["VIM-INV-IMPORT", "SAP → App", "SOAP (Erdat filter)", "MM / FI", "Live — manual /sendPost"],
            ["VIM-SES", "App ↔ SAP", "SOAP (PO line + qty)", "MM (SES)", "Live — sale order DAO"],
            ["VIM-WF-REMARKS", "SAP → App", "SOAP (Belnr)", "FI / Workflow", "Live — remarks sync"],
            ["VIM-SD-DEAL-POST", "App → SAP (legacy)", "HTTP form POST ZSO_CREATE_SRV", "SD", "Legacy code path — confirm use"],
        ],
    )

    doc.add_heading("1. Lead", level=1)
    doc.add_paragraph(
        "Velocity DMS and the embedded VIM backend call SAP through SOAP RFC web services exposed at "
        "/sap/bc/srt/rfc/sap/... (and one legacy HTTP service). The application never executes SAP GUI transaction codes; "
        "T-codes below are for SAP functional and basis teams to configure, verify master data, and troubleshoot the same "
        "objects the interfaces read or write."
    )

    doc.add_heading("2. Interface-to-T-code map", level=1)
    add_table(
        doc,
        [
            "App integration",
            "RFC / web service (Z)",
            "Build / test (SAP)",
            "Publish / transport",
            "Likely SAP source",
        ],
        [
            [
                "SAP-HCM-EMP",
                "ZHCM_EMP_INFO_PAD_WS",
                "SE37, SE80, PA20/PA30, PPOME",
                "SE09/SE10, STMS, SOAMANAGER, SICF",
                "PA0001, PA0002, PA0008, org assignment — to be confirmed by SAP team",
            ],
            [
                "VIM-INV-IMPORT",
                "ZKF_E_INVOICE_WEB_SERVICE (ZkfEInvoiceWebService)",
                "SE37, ME23N, ME2N, FB03",
                "SOAMANAGER, SICF, STMS",
                "EKKO/EKPO, vendor — mapping in SoapClientService.convertElementToSlsTblDeal",
            ],
            [
                "VIM-SES",
                "ZKF_E_INVOICE_SES_WEB_SERVICE",
                "SE37, MIGO, ML81N, ME23N",
                "SOAMANAGER, SICF",
                "Purchase order / SES — EBELN, EBELP in request",
            ],
            [
                "VIM-WF-REMARKS",
                "ZKF_INVOICE_WORKFLOW_REMARKS",
                "SE37, FB03, SWI1",
                "SOAMANAGER, SICF",
                "Invoice workflow text — Belnr, Stext, WiAagent in response",
            ],
            [
                "VIM-SD-DEAL-POST",
                "ZSO_CREATE_SRV (non-SICF URL)",
                "SE37, VA01, VA03",
                "SICF or ICM — to be confirmed",
                "SD sales order create — legacy SlsTblDealDAO",
            ],
        ],
    )

    doc.add_heading("3. Build, publish, and transport (SOAP RFC)", level=1)
    steps = [
        "SE37 — Verify Z function module exists and test with same parameters as web service (Erdat, EBELN, etc.).",
        "SE80 — Maintain function group and any custom includes for Z objects.",
        "SOAMANAGER — Create or review web service definition and binding for each Z* service.",
        "SICF — Activate service path under /sap/bc/srt/rfc/sap/<service>/<client>/... matching application.properties URLs (client 300 in current PRD samples).",
        "SM59 — HTTP connection to SAP app server if middleware calls SAP (optional; app uses direct URL).",
        "SU01 / PFCG — Technical user for Basic auth (properties sap.hcm.soap.* and soap.username — secrets not in this doc).",
        "SE09 / SE10 — Transport Z objects and SICF paths from dev to QA/PRD.",
        "STMS — Import transport queue to target system.",
        "SCC4 — Confirm client (e.g. 300) matches URL path segment in endpoint.",
    ]
    for s in steps:
        doc.add_paragraph(s, style="List Number")

    doc.add_heading("4. Interface detail", level=1)

    doc.add_heading("4.1 SAP-HCM-EMP (ZHCM_EMP_INFO_PAD_WS)", level=2)
    doc.add_paragraph(
        "Direction: SAP → App. Trigger: POST /velocity/syncSapHcmEmployeesFromSap (UI: Master Data → SAP Employees) "
        "or POST /velocity/syncSapHcmEmployeesFromXml (XML file upload). Auth: HTTP Basic (sap.hcm.soap.username). "
        "Timeouts: connect 60s, read 900s. Endpoint pattern: "
        "http(s)://<host>:8000/sap/bc/srt/rfc/sap/zhcm_emp_info_pad_ws/<client>/zhcm_emp_info_pad_ws/zhcm_emp_info_pad_ws"
    )
    add_table(
        doc,
        ["XML element (item)", "DB column (cfg_tbl_sap_hcm_employee)", "Verify in SAP (T-code)", "Notes"],
        [
            ["EMPLOYEE", "txt_employee_id", "PA20 / PA30", "Key; skip row if empty"],
            ["EMP_NAME", "txt_emp_name", "PA20", ""],
            ["DEPARTMENT_NAME", "txt_department_name", "PPOME / PA20", "Also upserts hr_tbl_department"],
            ["DIVISION", "txt_division", "PPOME", "Upserts hr_tbl_division"],
            ["DESIGNATION", "txt_designation", "PA20", ""],
            ["EMAIL", "txt_email", "PA20", ""],
            ["PAY_SCALE", "txt_pay_scale", "PA20 / PA30", "Infotype — confirm"],
            ["SUBAREA", "txt_subarea", "PPOME", "Upserts hr_tbl_subarea"],
            ["COMPANY_NAME", "txt_company_name", "PPOME", "Upserts hr_tbl_organization"],
            ["HIRING_DATE", "txt_hiring_date", "PA20", "Stored as string"],
            ["CONFIRMATION_DATE", "txt_confirmation_date", "PA20", ""],
            ["RETIREMENT_DATE", "txt_retirement_date", "PA20", ""],
            ["SEPARATION_DATE", "txt_separation_date", "PA20", ""],
            ["REHIRING_DATE", "txt_rehiring_date", "PA20", ""],
            ["ANNUAL_INC_TYPE", "txt_annual_inc_type", "PA20", ""],
            ["ACTIVE", "txt_active", "PA20", ""],
            ["REPORT_TO", "txt_report_to", "PA20", "Manager pernr"],
            ["REPORT_TO_NAME", "txt_report_to_name", "PA20", ""],
            ["PICTURE_BASE64", "txt_picture_path / bl_has_picture", "PA20", "File on disk under sap_hcm_employee_photos"],
            ["(all fields)", "txt_record_json", "—", "JSON snapshot without picture"],
        ],
    )
    doc.add_paragraph(
        "Side effect: SapHcmOrgStructureSyncService upserts hr_tbl_organization, hr_tbl_subarea, hr_tbl_division, hr_tbl_department from COMPANY_NAME, SUBAREA, DIVISION, DEPARTMENT_NAME."
    )
    doc.add_paragraph(
        "App user provisioning: SAP Employees UI can create cfg_tbl_user with createdFromSapHcm=true (manual; no automatic SAP write-back)."
    )

    doc.add_heading("4.2 VIM-INV-IMPORT (ZKF_E_INVOICE_WEB_SERVICE)", level=2)
    doc.add_paragraph(
        "Direction: SAP → App. Request: single field Erdat (yyyy-MM-dd). Trigger: POST /velocity/sendPost with JSON {\"erDate\":\"...\"}. "
        "Property: soap.endpoint.url. Response parsed into SlsTblDeal via convertElementToSlsTblDeal → sls_tbl_deal (and related customer/product)."
    )
    add_table(
        doc,
        ["Response XML tag", "App target", "SAP verify T-code", "Notes"],
        [
            ["Ebeln", "SlsTblDeal.txtDealNo", "ME23N", "PO number"],
            ["Ebelp", "Deal line item", "ME23N", ""],
            ["Aedat", "Deal dates", "ME23N", ""],
            ["Lifnr", "CfgTblCustomer.txtCustomerCode", "XK03 / FK03", "Vendor"],
            ["Vendor", "Customer name", "XK03", ""],
            ["Telf1", "Phone", "XK03", ""],
            ["Ort01", "City", "XK03", ""],
            ["Street", "Address", "XK03", ""],
            ["Email", "Email", "XK03", ""],
            ["Srvpos / Service", "Product / service", "ML81N", "To be confirmed"],
            ["Menge", "Quantity", "ME23N", ""],
            ["Netwr", "Net value", "ME23N", ""],
            ["TaxCode", "numSalesTaxSap", "FTXP / ME23N", ""],
        ],
    )

    doc.add_heading("4.3 VIM-SES (ZKF_E_INVOICE_SES_WEB_SERVICE)", level=2)
    doc.add_paragraph(
        "Direction: App → SAP (request) and SAP → App (response). Request fields: DOC_DATE, EBELN, EBELP, POST_DATE, QUANTITY. "
        "Invoked from SlsTblSaleOrderDAO via SoapClientService.sendSOAPRequestSES. Property: soap.endpoint.url.ses."
    )
    add_table(
        doc,
        ["SOAP field", "App usage", "SAP T-code", "Notes"],
        [
            ["EBELN", "Purchase order", "ME23N", ""],
            ["EBELP", "PO line", "ME23N", ""],
            ["DOC_DATE / POST_DATE", "Dates on order", "MIGO / ML81N", "Confirm with SAP"],
            ["QUANTITY", "SES quantity", "ML81N", ""],
        ],
    )

    doc.add_heading("4.4 VIM-WF-REMARKS (ZKF_INVOICE_WORKFLOW_REMARKS)", level=2)
    doc.add_paragraph(
        "Direction: SAP → App. Request: Belnr (document number) via Ziwfr. Property: soap.endpoint.url.remarks. "
        "Response ItOut/item fields update SlsTblSaleOrder remarks and approval levels."
    )
    add_table(
        doc,
        ["Response field", "App column", "SAP T-code", "Notes"],
        [
            ["Belnr", "txtSapInvoiceNo", "FB03", "Accounting document"],
            ["Stext", "txtTaxRemarks / txtAuditRemarks", "SWI1 / FB03", "Workflow text"],
            ["WiAagent", "Routing by department name", "SWI1", "e.g. Sale Tax"],
            ["WiCd / WiCt", "dteCreateddate", "SWI1", "Combined timestamp"],
        ],
    )

    doc.add_heading("4.5 VIM-SD-DEAL-POST (legacy)", level=2)
    doc.add_paragraph(
        "SlsTblDealDAO.addDealTOSAP posts form data to ServerConfiguration.ip_servre + /ZSO_CREATE_SRV. "
        "integeration_required flag in ServerConfiguration.java defaults false. Treat as legacy / to be confirmed before UAT."
    )

    doc.add_heading("5. Security", level=1)
    add_table(
        doc,
        ["Task", "T-code", "Notes"],
        [
            ["Service user", "SU01", "Users from soap.username and sap.hcm.soap.username — store in env, not in repo for PRD"],
            ["Roles", "PFCG", "RFC + SICF service auth for each Z FM"],
            ["Authorization test", "SU53", "After failed SOAP 401/403"],
            ["Auth trace", "ST01", "Role design for HCM read and MM/FI objects"],
            ["User audit", "SUIM", "Periodic review of integration users"],
        ],
    )
    doc.add_paragraph(
        "Authorization objects: exact objects depend on Z FM implementation — to be confirmed by SAP team (typical: "
        "S_RFC, S_SERVICE, HCM master read, M_BEST_* / F_BKPF_* for PO/invoice)."
    )

    doc.add_heading("6. Monitoring and troubleshooting", level=1)
    add_table(
        doc,
        ["Symptom", "Likely cause", "T-code / tool"],
        [
            ["HTTP 401", "Wrong Basic user/password", "SU01, SU53, check properties"],
            ["HTTP 404", "SICF service inactive or wrong URL/client", "SICF, SOAMANAGER"],
            ["HTTP 500", "ABAP runtime error in Z FM", "ST22, SM21"],
            ["Timeout (HCM read 900s)", "Large employee extract", "ST05, SM50, SM66, optimize Z FM"],
            ["SOAP fault", "FM exception or invalid Erdat/Belnr", "SE37, SRT_MONI"],
            ["Empty item list", "No data for selection date", "SE37 test, ME2N"],
            ["Partial org structure", "Missing COMPANY/SUBAREA in row", "PPOME, PA20"],
            ["SSL errors", "HTTPS mismatch", "STRUST, SMICM"],
        ],
    )

    doc.add_heading("7. Future / proposed (from code comments only)", level=1)
    doc.add_paragraph(
        "create_background_job_table.sql lists job types SAP_HCM_EMPLOYEE_SYNC and SAP_HCM_EMPLOYEE_XML_SYNC — not implemented in BackgroundJobTypes.java (only email jobs). "
        "ScheduledTask @Scheduled cron for invoice import is commented out; properties so.job.cron.expression / dep.job.cron.expression exist but no active @Scheduled reference found — to be confirmed."
    )
    doc.add_paragraph(
        "No write-back to SAP HCM (HR_INFOTYPE_OPERATION, BAPI_EMPLOYEE_*) is implemented. User create from SAP screen only writes to MySQL cfg_tbl_user."
    )

    doc.add_heading("8. UAT checklist", level=1)
    add_table(
        doc,
        ["Test case", "Set up in SAP", "Verify in SAP", "Verify in app"],
        [
            ["HCM full sync", "PA20 sample pernr has org/email", "PA20 / PPOME", "POST syncSapHcmEmployeesFromSap; cfg_tbl_sap_hcm_employee row"],
            ["HCM XML import", "Export sample XML from SE37", "SE37", "POST syncSapHcmEmployeesFromXml"],
            ["HCM photo", "Employee with photo in SAP", "PA20", "GET getSapHcmEmployeePhoto?employeeId="],
            ["Org hierarchy", "Valid company/subarea", "PPOME", "hr_tbl_organization / subarea / division / department"],
            ["Invoice import", "PO exists for Erdat", "ME2N", "POST /sendPost erDate; sls_tbl_deal"],
            ["SES posting", "Open PO line", "ME23N", "Sale order SES action; SOAP log"],
            ["Workflow remarks", "Invoice in workflow", "FB03 / SWI1", "Remarks on sls_tbl_sale_order"],
            ["Auth negative", "Lock user", "SU01", "Expect HTTP 401 on SOAP"],
        ],
    )

    doc.add_heading("9. T-code quick reference (alphabetical)", level=1)
    tcodes = [
        ("FB03", "Display document", "FI invoice / Belnr"),
        ("ME23N", "Display PO", "EKKO/EKPO for import & SES"),
        ("ME2N", "PO list by date", "Erdat import testing"),
        ("MIGO", "Goods movement", "SES related"),
        ("ML81N", "Service entry sheet", "SES"),
        ("PA20", "HR master data display", "Employee sync"),
        ("PA30", "HR master data maintain", "HCM test data"),
        ("PPOME", "Org structure", "Company/subarea/division"),
        ("SCC4", "Client maintenance", "Client 300 in URL"),
        ("SE09", "Transport organizer", "Release Z objects"),
        ("SE10", "Transport requests", "Import checklist"),
        ("SE37", "Function builder", "Test Z FMs"),
        ("SE80", "Object navigator", "Z development"),
        ("SICF", "HTTP service tree", "RFC SOAP paths"),
        ("SM21", "System log", "ICM / gateway errors"),
        ("SM37", "Background jobs", "If SAP-side batch used"),
        ("SM50", "Work processes", "Performance"),
        ("SM59", "RFC destinations", "Optional middleware"),
        ("SM66", "Global work proc overview", "Load"),
        ("SMICM", "ICM monitor", "HTTP/SOAP"),
        ("SOAMANAGER", "SOA management", "Web service bindings"),
        ("SRT_MONI", "Web service monitor", "SOAP runtime"),
        ("ST01", "Authorization trace", "SU53 follow-up"),
        ("ST05", "SQL trace", "Performance"),
        ("ST22", "ABAP dump", "HTTP 500"),
        ("STMS", "Transport management", "PRD import"),
        ("SU01", "User maintenance", "Integration users"),
        ("SU53", "Auth check", "Missing auth"),
        ("SUIM", "User information", "Audit"),
        ("SWI1", "Workflow inbox", "Remarks interface"),
        ("VA01", "Create sales order", "Legacy ZSO_CREATE_SRV"),
        ("VA03", "Display sales order", "Legacy SD"),
        ("XK03", "Display vendor", "Lifnr on deals"),
    ]
    tcodes.sort(key=lambda x: x[0])
    add_table(doc, ["T-code", "Name", "Used for"], list(tcodes))

    doc.add_heading("Open items for SAP team", level=1)
    open_items = [
        "Confirm ZHCM_EMP_INFO_PAD_WS ABAP source (tables/infotypes) for each XML field.",
        "Confirm SAP client and host for each environment (PRD sample uses client 300; HCM host prodapps.qarshi.com vs FI host 192.0.2.150 in properties).",
        "Provide WSDL/XSD for all Z web services and whether SOAPAction header is required (sap.hcm.soap.soap-action empty in config).",
        "Confirm whether VIM invoice/SES/remark interfaces are still in scope for Velocity DMS go-live or HCM-only.",
        "Confirm authorization roles for integration users (read HCM, MM PO, FI document, workflow).",
        "Clarify legacy ZSO_CREATE_SRV and ServerConfiguration.integeration_required usage.",
        "Implement or drop planned background jobs SAP_HCM_EMPLOYEE_SYNC in BackgroundJobTypes.",
        "Rotate and remove plaintext SAP passwords from application.properties; use environment variables.",
    ]
    for item in open_items:
        doc.add_paragraph(item, style="List Bullet")

    doc.add_heading("Code references", level=1)
    add_table(
        doc,
        ["Topic", "File(s)"],
        [
            ["HCM SOAP client", "VIM-BE/.../SapHcmSoapClient.java"],
            ["HCM import & field map", "VIM-BE/.../SapHcmEmployeeService.java"],
            ["HCM org upsert", "VIM-BE/.../SapHcmOrgStructureSyncService.java"],
            ["HCM REST API", "VIM-BE/.../SapHcmEmployeeController.java"],
            ["HCM DB schema", "VIM-BE/scripts/create_sap_hcm_employee_tables.sql"],
            ["Config endpoints", "VIM-BE/src/main/resources/application.properties"],
            ["VIM SOAP client", "VIM-BE/.../SoapClientService.java"],
            ["Manual SAP jobs", "VIM-BE/.../SoapController.java, ScheduledTask.java"],
            ["Environment doc", "VIM-BE/STAGING_AND_PRODUCTION_APIS.md, api-environments.md"],
            ["FE SAP Employees UI", "VIM-FE/.../sap-hcm-employees.component.ts"],
        ],
    )

    doc.save(OUT)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
