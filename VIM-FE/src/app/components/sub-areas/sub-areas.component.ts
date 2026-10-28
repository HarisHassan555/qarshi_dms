import { Component, OnInit } from '@angular/core';
import { OrgStructureService } from 'src/app/services/org-structure/org-structure.service';

@Component({
    selector: 'app-sub-areas',
    templateUrl: './sub-areas.component.html',
    styleUrls: ['./sub-areas.component.css'],
})
export class SubAreasComponent implements OnInit {
    search = '';
    loading = false;
    loadError = '';
    rows: any[] = [];

    cols = [
        { field: 'txtSubareaName', title: 'Sub Area' },
        { field: 'organizationName', title: 'Organization' },
        { field: 'txtSubareaCode', title: 'Code' },
        { field: 'blnStatus', title: 'Status', width: '100px' },
    ];

    constructor(private orgStructureService: OrgStructureService) {}

    ngOnInit(): void {
        this.load();
    }

    load(): void {
        this.loading = true;
        this.loadError = '';
        this.orgStructureService.getSubareas().subscribe({
            next: (data) => {
                this.rows = data || [];
                this.loading = false;
            },
            error: () => {
                this.loadError = 'Could not load sub areas.';
                this.rows = [];
                this.loading = false;
            },
        });
    }
}
