import { Component, ViewEncapsulation, OnInit, OnDestroy, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

import { CommonModule } from '@angular/common';
import { urls } from 'src/app/utils/urls';

@Component({
    selector: 'app-abc',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './abc.component.html',
    styleUrls: ['./abc.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class AbcComponent implements OnInit, OnDestroy, OnChanges {
    @Input() formData: any = {};
    @Input() formFields: any[] = [];
    @Input() application: any = null;
    @Input() isEmbedded: boolean = false;

    signatureSlots: Array<{
        label: string;
        order: number;
        departmentId?: number;
        signatureUrl: string;
        approvedDateText: string;
    }> = [];

    private readonly fallbackSignatureSlots: Array<{
        label: string;
        keywords: string[];
    }> = [
        { label: 'User Deptt. (HoD)', keywords: ['user dept', 'hod'] },
        { label: 'Technical Expert', keywords: ['technical', 'expert'] },
        { label: 'Procurement', keywords: ['procurement'] },
        { label: 'Finance', keywords: ['finance'] },
        { label: 'Core Team HTR. / CCT HO', keywords: ['core team', 'htr', 'cct', 'ho'] },
    ];

    private approvalHistory: any[] = [];

    // Session storage key for persistence across page refresh/new tabs
    private readonly SESSION_KEY = 'abc_form_state';
    private navigationState: any = null;

    constructor(
        private router: Router,
        private route: ActivatedRoute
    ) {
        // CRITICAL: Capture navigation state in constructor
        // This is the ONLY reliable way to get navigation state before ngOnInit
        const navigation = this.router.getCurrentNavigation();
        if (navigation?.extras?.state) {
            console.log("✅ [Constructor] Navigation state captured:", navigation.extras.state);
            this.navigationState = navigation.extras.state;
            // Save to sessionStorage so it persists across page refresh
            this.saveToSessionStorage(this.navigationState);
        } else {
            console.warn("⚠️  [Constructor] No navigation state available (may be page refresh or direct URL)");
        }
    }

    ngOnInit() {
        console.log("───────────────────────────────");
        console.log("🔍 [ngOnInit] Starting form initialization...");

        // Strategy 0: Use Inputs if provided
        if (Object.keys(this.formData).length > 0) {
            console.log("✅ [ngOnInit] Using provided @Input data");
            // Automatically triggers populateForm via standard lifecycle or explicit call if needed
        } else if (this.navigationState) {
            // Strategy 1: Use navigation state captured in constructor
            console.log("✅ [ngOnInit] Using state from constructor");
            this.loadFromNavigationState(this.navigationState);
        } else {
            // Strategy 2: Try history.state (browser back/forward)
            const historyState = history.state;
            if (historyState && (historyState.formData || historyState.formFields)) {
                console.log("✅ [ngOnInit] Using state from history.state (back/forward navigation)");
                this.loadFromNavigationState(historyState);
            } else {
                // Strategy 3: Try sessionStorage (page refresh, new tab)
                const sessionState = this.loadFromSessionStorage();
                if (sessionState) {
                    console.log("✅ [ngOnInit] Using state from sessionStorage (page refresh/new tab)");
                    this.loadFromNavigationState(sessionState);
                } else {
                    console.warn("⚠️  [ngOnInit] No form data found in any source (first visit or data cleared)");
                }
            }
        }

        // Always log final state for debugging
        console.log("formData keys:", Object.keys(this.formData));
        console.log("formData (pretty):", JSON.stringify(this.formData, null, 2));
        if (this.formFields?.length) {
            console.log("formFields labels & keys:",
                this.formFields.map(f => ({
                    label: f.label,
                    possibleKey: f.serFieldId || f.key || f.name || f.id || "—",
                    type: f.type
                }))
            );
        }
        console.log("───────────────────────────────");

        // Only populate form if we have actual data
        if (Object.keys(this.formData).length > 0 || this.formFields?.length > 0) {
            this.populateForm();
        } else {
            console.info("ℹ️  [ngOnInit] No form data to populate");
        }
        this.refreshSignatureSlots();
    }

    /**
     * Load form data from any navigation state source
     * Centralizes the logic for setting formData, formFields, and application
     */
    private loadFromNavigationState(state: any) {
        if (!state) return;

        this.formData = state['formData'] || {};
        this.formFields = state['formFields'] || [];
        this.application = state['application'] || null;

        console.log("📋 [loadFromNavigationState] Loaded data:", {
            formDataKeys: Object.keys(this.formData).length,
            formFieldsCount: this.formFields.length,
            hasApplication: !!this.application
        });
    }

    /**
     * Save form state to sessionStorage for persistence across refresh/new tabs
     * SessionStorage is cleared when the browser tab closes (auto cleanup)
     */
    private saveToSessionStorage(state: any) {
        try {
            sessionStorage.setItem(this.SESSION_KEY, JSON.stringify(state));
            console.log("💾 [saveToSessionStorage] Form state saved to sessionStorage");
        } catch (error) {
            console.error("❌ [saveToSessionStorage] Failed to save to sessionStorage:", error);
        }
    }

    /**
     * Load form state from sessionStorage
     * Returns null if not available or if parsing fails
     */
    private loadFromSessionStorage(): any {
        try {
            const stored = sessionStorage.getItem(this.SESSION_KEY);
            if (stored) {
                const state = JSON.parse(stored);
                console.log("📂 [loadFromSessionStorage] Form state retrieved from sessionStorage");
                return state;
            }
        } catch (error) {
            console.error("❌ [loadFromSessionStorage] Failed to load from sessionStorage:", error);
        }
        return null;
    }

    /**
     * CLEANUP METHOD: Call this when the multi-step form wizard completes
     * This removes the form data from sessionStorage to prevent stale data
     *
     * Usage in your submit/complete handler:
     * this.clearSessionStorage();
     * this.router.navigate(['/success']);
     */
    clearSessionStorage() {
        try {
            sessionStorage.removeItem(this.SESSION_KEY);
            console.log("🧹 [clearSessionStorage] Form state cleared from sessionStorage");
        } catch (error) {
            console.error("❌ [clearSessionStorage] Failed to clear sessionStorage:", error);
        }
    }

    ngOnDestroy() {
        // Optional: Log when component is destroyed
        console.log("👋 [ngOnDestroy] ABC component destroyed");
    }

    ngOnChanges(changes: SimpleChanges) {
        if (changes['formData'] || changes['formFields']) {
            console.log("🔄 [ngOnChanges] Inputs updated", {
                formDataRaw: this.formData,
                fieldsCount: this.formFields?.length
            });

            if (Object.keys(this.formData || {}).length > 0 || (this.formFields && this.formFields.length > 0)) {
                this.populateForm();
            }
        }

        if (changes['application']) {
            this.refreshSignatureSlots();
        }
    }

    private refreshSignatureSlots() {
        this.approvalHistory = this.parseApprovalHistory();
        const pipelines = this.getPipelineData();
        if (pipelines.length > 0) {
            this.signatureSlots = pipelines.map((pipeline: any, index: number) => {
                const order = pipeline.intApprovalOrder || (index + 1);
                const departmentId = pipeline.hrTblDepartment?.serDepartmentId || pipeline.serDepartmentId || pipeline.departmentId;
                const entry = this.getApprovalEntryForPipeline(order, departmentId);
                const label =
                    pipeline.hrTblDepartment?.txtDepartmentName ||
                    pipeline.departmentName ||
                    pipeline.txtDepartmentName ||
                    entry?.departmentName ||
                    `Department ${order}`;
                const userId = entry?.approvedBy || entry?.approverUserId || entry?.userId;
                const approvedDate = entry?.approvedDate;
                const hasSignature = !!entry?.signaturePath;
                return {
                    label,
                    order,
                    departmentId,
                    signatureUrl: userId && hasSignature ? `${urls.API_URL}getSignature?userId=${userId}` : '',
                    approvedDateText: hasSignature ? this.formatApprovalDate(approvedDate) : ''
                };
            });
            return;
        }

        this.signatureSlots = this.fallbackSignatureSlots.map((slot, index) => {
            const entry = this.getApprovalEntryForSlot(slot);
            const userId = entry?.approvedBy || entry?.approverUserId || entry?.userId;
            const approvedDate = entry?.approvedDate;
            const hasSignature = !!entry?.signaturePath;
            return {
                label: slot.label,
                order: index + 1,
                signatureUrl: userId && hasSignature ? `${urls.API_URL}getSignature?userId=${userId}` : '',
                approvedDateText: hasSignature ? this.formatApprovalDate(approvedDate) : ''
            };
        });
    }

    private parseApprovalHistory(): any[] {
        try {
            const historyJson = this.application?.txtApprovalHistory;
            if (historyJson) {
                const parsed = JSON.parse(historyJson);
                return Array.isArray(parsed) ? parsed : [];
            }
        } catch (e) {
            return [];
        }
        return [];
    }

    private getApprovalEntryForSlot(slot: { keywords: string[] }): any | null {
        if (!this.approvalHistory || this.approvalHistory.length === 0) {
            return null;
        }

        const keywordsLower = (slot.keywords || []).map(k => k.toLowerCase());
        const byDept = this.approvalHistory.find((e: any) => {
            const deptName = (e.departmentName || '').toString().toLowerCase();
            const roleName = (e.role || '').toString().toLowerCase();
            const combined = `${deptName} ${roleName}`.trim();
            if (!combined) return false;
            return keywordsLower.every(k => combined.includes(k));
        });
        if (byDept) {
            return byDept;
        }

        return null;
    }

    private getApprovalEntryForPipeline(order: number, departmentId?: number, departmentName?: string): any | null {
        if (!this.approvalHistory || this.approvalHistory.length === 0) {
            return null;
        }

        let entry = null;
        if (departmentId) {
            entry = this.approvalHistory.find((e: any) =>
                (e.level === order || e.intApprovalOrder === order) &&
                (Number(e.departmentId) === Number(departmentId) || Number(e.serDepartmentId) === Number(departmentId))
            );
        }
        if (!entry) {
            entry = this.approvalHistory.find((e: any) => e.level === order || e.intApprovalOrder === order);
        }
        if (!entry && departmentId) {
            entry = this.approvalHistory.find((e: any) =>
                Number(e.departmentId) === Number(departmentId) || Number(e.serDepartmentId) === Number(departmentId)
            );
        }
        if (!entry && departmentName) {
            const nameLower = departmentName.toLowerCase();
            entry = this.approvalHistory.find((e: any) =>
                (e.departmentName || '').toString().toLowerCase() === nameLower
            );
        }

        return entry || null;
    }

    private getPipelineData(): any[] {
        if (!this.application || !this.application.cfgTblCustomForm) {
            return [];
        }
        const form = this.application.cfgTblCustomForm;

        let pipelines = form.approvalPipelines || form.cfgTblCustomFormApprovalPipelines;
        if ((!pipelines || !Array.isArray(pipelines) || pipelines.length === 0) && form.txtApprovalPipeline) {
            try {
                pipelines = JSON.parse(form.txtApprovalPipeline);
            } catch {
                return [];
            }
        }
        if (!pipelines || !Array.isArray(pipelines) || pipelines.length === 0) {
            return [];
        }
        return [...pipelines].sort((a: any, b: any) =>
            (a.intApprovalOrder || 0) - (b.intApprovalOrder || 0)
        );
    }

    private formatApprovalDate(dateValue: any): string {
        if (!dateValue) return '';
        try {
            const dt = new Date(dateValue);
            if (isNaN(dt.getTime())) return String(dateValue);
            return dt.toLocaleString();
        } catch (e) {
            return String(dateValue);
        }
    }

    populateForm() {
        if (!this.formFields || this.formFields.length === 0) {
            return;
        }

        // Map form fields to abc component properties
        // This will be used to populate the HTML template
        this.mapFormDataToFields();
    }

    mapFormDataToFields() {
        // Helper method to get field value by label
        const getFieldValue = (label: string): any => {
            // Try exact match first
            if (this.formData[label] !== undefined && this.formData[label] !== null && this.formData[label] !== '') {
                return this.formData[label];
            }

            // Convert label to snake_case key — this is the key format used when the form was submitted
            const derivedKey = this.getFieldName(label);
            if (this.formData[derivedKey] !== undefined && this.formData[derivedKey] !== null && this.formData[derivedKey] !== '') {
                return this.formData[derivedKey];
            }

            // Try case-insensitive match
            const lowerLabel = label.toLowerCase();
            for (const key in this.formData) {
                if (key.toLowerCase() === lowerLabel) {
                    return this.formData[key];
                }
            }

            // Try matching by field label from formFields
            const field = this.formFields.find(f =>
                f.label && f.label.toLowerCase() === lowerLabel
            );
            if (field) {
                // Try snake_case label key first
                const fieldName = this.getFieldName(field.label);
                if (this.formData[fieldName] !== undefined && this.formData[fieldName] !== null) {
                    return this.formData[fieldName];
                }
                // Fallback: try field_${serFieldId} — matches how form submission saves data
                const fieldIdKey = `field_${field.serFieldId}`;
                if (this.formData[fieldIdKey] !== undefined && this.formData[fieldIdKey] !== null) {
                    return this.formData[fieldIdKey];
                }
            }

            // Last resort: scan all formFields, try field_ID key for each
            for (const f of this.formFields) {
                if (!f.label) continue;
                if (f.label.toLowerCase().includes(lowerLabel) || lowerLabel.includes(f.label.toLowerCase())) {
                    const fieldIdKey = `field_${f.serFieldId}`;
                    if (this.formData[fieldIdKey] !== undefined && this.formData[fieldIdKey] !== null) {
                        return this.formData[fieldIdKey];
                    }
                }
            }

            return null;
        };

        // Store mapped values for template access
        (this as any).mappedData = {};

        // Updated field mappings to match actual form fields from database
        const fieldMappings: { [key: string]: string[] } = {
            'division': ['DIVISION', 'Division', 'division', 'DIVISION / DEPARTMENT', 'Division/Department'],
            'department': ['DEPARTMENT', 'Department', 'department'],
            'section': ['SECTION', 'Section', 'section'],
            'capfNumber': ['CAPF #', 'CAPF', 'capf', 'CAPF Number'],
            'date': ['Date', 'DATE', 'date', 'Submission Date'],
            'assetName': ['NAME OF ASSET', 'Asset Name', 'asset name', 'NAME OF ASSET / ITEM', 'Name of Asset/Item'],
            'specification': ['DETAIL SPECIFICATION', 'Specification', 'specification', 'DETAIL SPECIFICATION', 'Details & Specification', 'Details Specification'],
            'utility': ['UTILITY', 'Utility', 'utility', 'UTILITY & PURPOSE', 'Utility & Purpose'],
            'purpose': ['PURPOSE', 'Purpose', 'purpose'],
            'feasibilityReport': ['FEASIBILITY REPORT', 'Feasibility Report', 'feasibility report', 'FEASIBILITY REPORT ATTACHED', 'feasibility_attached_report'],
            'reason': ['REASON', 'Reason', 'reason', 'IF NO THEN MENTION REASON', 'IF NO THEN MENTION REASON:'],
            'vendorName': ['NAME', 'Vendor Name', 'vendor name', 'NAME'],
            'vendorAddress': ['ADDRESS', 'Address', 'address', 'ADDRESS'],
            'approvedPrice': ['APPROVED PRICE', 'Approved Price', 'approved price', 'APPROVED PRICE', 'Approved price'],
            'deliveryPeriod': ['DELIVERY PERIOD', 'Delivery Period', 'delivery period', 'DELIVERY PERIOD & DATE', ' delivery period'],
            'deliveryDate': ['Delivery Date', 'DELIVERY DATE', 'delivery date', 'Delivery Date'],
            'termsConditions': ['TERMS & CONDITIONS', 'Terms & Conditions', 'terms conditions', 'TERMS & CONDITIONS'],
            'thirdPartyAssessment': ['Thrid party assessment carried out', 'Third party assessment carried out', 'THIRD PARTY ASSESSMENT', 'Third Party Assessment']
        };

        for (const [key, possibleLabels] of Object.entries(fieldMappings)) {
            for (const label of possibleLabels) {
                const value = getFieldValue(label);
                if (value !== null && value !== undefined && value !== '') {
                    (this as any).mappedData[key] = value;
                    console.log(`✅ [mapFormDataToFields] Mapped '${label}' → ${key} = `, value);
                    break;
                }
            }
        }

        console.log("🗺️  [mapFormDataToFields] Final mapped data:", (this as any).mappedData);
    }

    getFieldName(label: string): string {
        return label.toLowerCase()
            .replace(/[^a-z0-9]+/g, '_')
            .replace(/^_+|_+$/g, '');
    }

    getFieldValue(fieldLabel: string): string {
        if (!this.formData || Object.keys(this.formData).length === 0) {
            return '';
        }

        // Try to get value from mapped data first
        const fieldMappings: { [key: string]: string } = {
            'DIVISION / DEPARTMENT': 'division',
            'Division/Department': 'division',
            'CAPF #': 'capfNumber',
            'Date': 'date',
            'NAME OF ASSET / ITEM': 'assetName',
            'Name of Asset/Item': 'assetName',
            'DETAIL SPECIFICATION': 'specification',
            'Details & Specification': 'specification',
            'UTILITY & PURPOSE': 'utility',
            'Utility & Purpose': 'utility',
            'FEASIBILITY REPORT ATTACHED': 'feasibilityReport',
            'feasibility_attached_report': 'feasibilityReport',
            'IF NO THEN MENTION REASON': 'reason',
            'IF NO THEN MENTION REASON:': 'reason',
            'NAME': 'vendorName',
            'Vendor Name': 'vendorName',
            'ADDRESS': 'vendorAddress',
            'APPROVED PRICE': 'approvedPrice',
            'Approved price': 'approvedPrice',
            'DELIVERY PERIOD & DATE': 'deliveryPeriod',
            ' delivery period': 'deliveryPeriod',
            'Delivery Date': 'deliveryDate',
            'TERMS & CONDITIONS': 'termsConditions',
            'Thrid party assessment carried out': 'thirdPartyAssessment'
        };

        const mappedKey = fieldMappings[fieldLabel];
        if (mappedKey && (this as any).mappedData && (this as any).mappedData[mappedKey]) {
            const value = (this as any).mappedData[mappedKey];
            return value !== null && value !== undefined ? String(value) : '';
        }

        // Alias map: template label → exact formData key (for cases where they don't match)
        const labelAliasMap: { [label: string]: string } = {
            'DETAIL SPECIFICATION': 'details_specification',
            'NAME': 'vendor_name',
            'ADDRESS': 'vendor_address',
            'DELIVERY PERIOD & DATE': 'delivery_period',
            'IF NO THEN MENTION REASON:': 'if_no_then_mention_reason',
            'IF NO THEN MENTION REASON': 'if_no_then_mention_reason',
            'UTILITY & PURPOSE': 'utility_purpose',
            'TERMS & CONDITIONS': 'terms_conditions',
            'FEASIBILITY REPORT ATTACHED': 'feasibility_report_attached',
            // Third party field - try all possible key variants (typo in original label)
            'Third Party assessment carried out': 'thrid_party_assessment_carried_out',
            'Thrid party assessment carried out': 'thrid_party_assessment_carried_out',
            'THIRD PARTY ASSESSMENT': 'thrid_party_assessment_carried_out',
        };
        const aliasKey = labelAliasMap[fieldLabel];
        if (aliasKey && this.formData[aliasKey] !== undefined && this.formData[aliasKey] !== null) {
            const av = this.formData[aliasKey];
            // Skip file/object values (e.g. uploaded file object stored as feasibility report)
            if (typeof av === 'string' || typeof av === 'number') return String(av);
        }

        // Try direct lookup in formData
        if (this.formData[fieldLabel] !== undefined && this.formData[fieldLabel] !== null) {
            return String(this.formData[fieldLabel]);
        }

        // Convert label to snake_case key — this is how form submission saves field values
        // e.g., 'DIVISION / DEPARTMENT' → 'division_department'
        const derivedKey = this.getFieldName(fieldLabel);
        if (this.formData[derivedKey] !== undefined && this.formData[derivedKey] !== null) {
            return String(this.formData[derivedKey]);
        }

        // Try case-insensitive lookup
        const lowerLabel = fieldLabel.toLowerCase().trim();
        for (const key in this.formData) {
            if (key.toLowerCase().trim() === lowerLabel) {
                const value = this.formData[key];
                return value !== null && value !== undefined ? String(value) : '';
            }
        }

        // Try matching by formFields - check if any form field label matches
        if (this.formFields && this.formFields.length > 0) {
            const field = this.formFields.find(f => {
                if (!f.label) return false;
                const fieldLabelLower = f.label.toLowerCase().trim();
                // Try exact match
                if (fieldLabelLower === lowerLabel) return true;
                // Try partial match (e.g., "NAME OF ASSET" matches "NAME OF ASSET / ITEM")
                if (fieldLabelLower.includes(lowerLabel) || lowerLabel.includes(fieldLabelLower)) return true;
                return false;
            });

            if (field) {
                // Try snake_case label key
                const fieldName = this.getFieldName(field.label);
                const v1 = this.formData[fieldName];
                if (v1 !== undefined && v1 !== null) return String(v1);

                // Fallback: try field_${serFieldId} — matches how form submission saves data
                const fieldIdKey = `field_${field.serFieldId}`;
                const v2 = this.formData[fieldIdKey];
                if (v2 !== undefined && v2 !== null) return String(v2);
            }
        }

        // Final fallback: scan all formFields by partial label match, try field_ID key
        if (this.formFields && this.formFields.length > 0) {
            for (const f of this.formFields) {
                if (!f.label) continue;
                const fl = f.label.toLowerCase().trim();
                if (fl.includes(lowerLabel) || lowerLabel.includes(fl)) {
                    const fieldIdKey = `field_${f.serFieldId}`;
                    const v = this.formData[fieldIdKey];
                    if (v !== undefined && v !== null) return String(v);
                }
            }
        }

        return '';
    }

    getCapfNumber(): string {
        return this.getFieldValue('CAPF #') ||
            (this.application?.txtFormCode || '');
    }

    getDate(): string {
        const dateValue = this.getFieldValue('Date') ||
            (this.application?.dteCreatedDate || '');
        if (dateValue) {
            try {
                const date = new Date(dateValue);
                return date.toLocaleDateString();
            } catch (e) {
                return dateValue;
            }
        }
        return '';
    }

    /**
     * Check if feasibility report is attached
     * Returns true if field has a value (file attached, true, yes, etc.)
     * Returns false if field is empty/null/false/no
     */
    isFeasibilityReportAttached(): boolean {
        // Read directly from formData using known key
        const rawValue = this.formData['feasibility_report_attached'] ??
            (this as any).mappedData?.feasibilityReport;

        if (rawValue === undefined || rawValue === null || rawValue === '') {
            return false;
        }

        // If it's a file object (uploaded file means "Yes, report is attached")
        if (typeof rawValue === 'object' && rawValue.name) {
            return true;
        }

        // Plain string value
        const str = String(rawValue).toLowerCase().trim();
        if (str === 'no' || str === 'false' || str === '0' || str === 'n') {
            return false;
        }
        if (str === 'yes' || str === 'true' || str === '1' || str === 'y') {
            return true;
        }

        // Non-empty string / any other truthy value → attached
        return str.length > 0;
    }

    /**
     * Get the yes/no checkbox state for feasibility report
     * Used for styling the checkboxes
     */
    isFeasibilityYesChecked(): boolean {
        return this.isFeasibilityReportAttached();
    }

    isFeasibilityNoChecked(): boolean {
        return !this.isFeasibilityReportAttached();
    }

    /**
     * Show reason field only if feasibility report is NOT attached
     */
    shouldShowFeasibilityReason(): boolean {
        return !this.isFeasibilityReportAttached();
    }

    /**
     * Get the reason text (only populated if feasibility is "No")
     */
    getFeasibilityReason(): string {
        if (this.shouldShowFeasibilityReason()) {
            return this.getFieldValue('IF NO THEN MENTION REASON:') || '';
        }
        return '';
    }

    /**
     * Get the value for Third Party Assessment
     */
    getThirdPartyAssessmentValue(): string {
        // Try all possible key variants (original field label had a typo "Thrid" instead of "Third")
        const possibleKeys = [
            'thrid_party_assessment_carried_out', // typo variant — most likely stored key
            'third_party_assessment_carried_out',
            'third_party_assessment',
            'thrid_party_assessment',
        ];
        for (const key of possibleKeys) {
            if (this.formData[key] !== undefined && this.formData[key] !== null && this.formData[key] !== '') {
                return String(this.formData[key]).toLowerCase().trim();
            }
        }

        // Fallback to mappedData
        if ((this as any).mappedData?.thirdPartyAssessment) {
            return String((this as any).mappedData.thirdPartyAssessment).toLowerCase().trim();
        }
        return '';
    }

    isThirdPartyYesChecked(): boolean {
        const value = this.getThirdPartyAssessmentValue();
        return value === 'yes' || value === 'y' || value === 'true' || value === '1';
    }

    isThirdPartyNoChecked(): boolean {
        const value = this.getThirdPartyAssessmentValue();
        return value === 'no' || value === 'n' || value === 'false' || value === '0';
    }

    isThirdPartyNAChecked(): boolean {
        const value = this.getThirdPartyAssessmentValue();
        return value === 'na' || value === 'n/a' || value === 'not applicable';
    }
}



