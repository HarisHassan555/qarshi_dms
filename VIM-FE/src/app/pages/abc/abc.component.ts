import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
    selector: 'app-abc',
    standalone: true,
    templateUrl: './abc.component.html',
    styleUrls: ['./abc.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class AbcComponent implements OnInit {
    formData: any = {};
    formFields: any[] = [];
    application: any = null;

    constructor(
        private router: Router,
        private route: ActivatedRoute
    ) {}

    ngOnInit() {
        // Get data from navigation state
        const navigation = this.router.getCurrentNavigation();
        if (navigation?.extras?.state) {
            this.formData = navigation.extras.state['formData'] || {};
            this.formFields = navigation.extras.state['formFields'] || [];
            this.application = navigation.extras.state['application'] || null;
            this.populateForm();
        } else {
            // Try to get from history state (for browser back/forward)
            const historyState = history.state;
            if (historyState && historyState.formData) {
                this.formData = historyState.formData || {};
                this.formFields = historyState.formFields || [];
                this.application = historyState.application || null;
                this.populateForm();
            }
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
            if (this.formData[label]) {
                return this.formData[label];
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
                const fieldName = this.getFieldName(field.label);
                return this.formData[fieldName];
            }
            
            return null;
        };

        // Store mapped values for template access
        (this as any).mappedData = {};
        
        // Common CAPF field mappings
        const fieldMappings: { [key: string]: string[] } = {
            'division': ['DIVISION', 'Division', 'division', 'DIVISION / DEPARTMENT'],
            'department': ['DEPARTMENT', 'Department', 'department'],
            'section': ['SECTION', 'Section', 'section'],
            'capfNumber': ['CAPF #', 'CAPF', 'capf', 'CAPF Number'],
            'date': ['Date', 'DATE', 'date', 'Submission Date'],
            'assetName': ['NAME OF ASSET', 'Asset Name', 'asset name', 'NAME OF ASSET / ITEM'],
            'specification': ['DETAIL SPECIFICATION', 'Specification', 'specification', 'DETAIL SPECIFICATION'],
            'utility': ['UTILITY', 'Utility', 'utility', 'UTILITY & PURPOSE'],
            'purpose': ['PURPOSE', 'Purpose', 'purpose'],
            'feasibilityReport': ['FEASIBILITY REPORT', 'Feasibility Report', 'feasibility report', 'FEASIBILITY REPORT ATTACHED'],
            'reason': ['REASON', 'Reason', 'reason', 'IF NO THEN MENTION REASON'],
            'vendorName': ['NAME', 'Vendor Name', 'vendor name', 'NAME'],
            'vendorAddress': ['ADDRESS', 'Address', 'address', 'ADDRESS'],
            'approvedPrice': ['APPROVED PRICE', 'Approved Price', 'approved price', 'APPROVED PRICE'],
            'deliveryPeriod': ['DELIVERY PERIOD', 'Delivery Period', 'delivery period', 'DELIVERY PERIOD & DATE'],
            'termsConditions': ['TERMS & CONDITIONS', 'Terms & Conditions', 'terms conditions', 'TERMS & CONDITIONS']
        };

        for (const [key, possibleLabels] of Object.entries(fieldMappings)) {
            for (const label of possibleLabels) {
                const value = getFieldValue(label);
                if (value !== null && value !== undefined && value !== '') {
                    (this as any).mappedData[key] = value;
                    break;
                }
            }
        }
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
            'CAPF #': 'capfNumber',
            'Date': 'date',
            'NAME OF ASSET / ITEM': 'assetName',
            'DETAIL SPECIFICATION': 'specification',
            'UTILITY & PURPOSE': 'utility',
            'FEASIBILITY REPORT ATTACHED': 'feasibilityReport',
            'IF NO THEN MENTION REASON': 'reason',
            'NAME': 'vendorName',
            'ADDRESS': 'vendorAddress',
            'APPROVED PRICE': 'approvedPrice',
            'DELIVERY PERIOD & DATE': 'deliveryPeriod',
            'TERMS & CONDITIONS': 'termsConditions'
        };

        const mappedKey = fieldMappings[fieldLabel];
        if (mappedKey && (this as any).mappedData && (this as any).mappedData[mappedKey]) {
            const value = (this as any).mappedData[mappedKey];
            return value !== null && value !== undefined ? String(value) : '';
        }

        // Try direct lookup in formData
        if (this.formData[fieldLabel] !== undefined && this.formData[fieldLabel] !== null) {
            return String(this.formData[fieldLabel]);
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
                const fieldName = this.getFieldName(field.label);
                const value = this.formData[fieldName];
                if (value !== undefined && value !== null) {
                    return String(value);
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
}
