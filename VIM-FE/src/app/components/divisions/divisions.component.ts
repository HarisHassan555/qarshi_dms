import { Component, OnInit } from '@angular/core';
import { OrgStructureService } from 'src/app/services/org-structure/org-structure.service';

@Component({
    selector: 'app-divisions',
    templateUrl: './divisions.component.html',
    styleUrls: ['./divisions.component.css'],
})
export class DivisionsComponent implements OnInit {
    search = '';
    loading = false;
    loadError = '';
    rows: any[] = [];

    cols = [
        { field: 'txtDivisionName', title: 'Division' },
        { field: 'subareaName', title: 'Sub Area' },
        { field: 'organizationName', title: 'Organization' },
        { field: 'txtDivisionCode', title: 'Code' },
        { field: 'blnStatus', title: 'Status', width: '100px' },
    ];

    constructor(private orgStructureService: OrgStructureService) {}

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading = true;
        this.loadError = '';
        this.orgStructureService.getDivisions().subscribe({
            next: (data) => {
                this.rows = data || [];
                this.loading = false;
            },
            error: () => {
                this.loadError = 'Could not load divisions.';
                this.rows = [];
                this.loading = false;
            },
        });
    }
}
