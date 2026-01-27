import { Component } from '@angular/core';
import { animate, style, transition, trigger } from '@angular/animations';
import { SharedDataService } from './services/shared-data/shared-data.service';

@Component({
    moduleId: module.id,
    templateUrl: './finance.html',
    animations: [
        trigger('toggleAnimation', [
            transition(':enter', [style({ opacity: 0, transform: 'scale(0.95)' }), animate('100ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))]),
            transition(':leave', [animate('75ms', style({ opacity: 0, transform: 'scale(0.95)' }))]),
        ]),
    ],
})
export class FinanceComponent {
    user: any;
    username: string = '';

    constructor(private sharedDataService: SharedDataService) {
        this.getUser();
    }

    ngOnInit() {
        // Get user from localStorage as fallback
        const userJson = localStorage.getItem('user');
        if (userJson) {
            try {
                const user = JSON.parse(userJson);
                this.username = user.txtUserName || '';
            } catch (e) {
                console.error('Error parsing user data:', e);
            }
        }
    }

    getUser() {
        this.sharedDataService.getUser().subscribe(data => {
            if (data) {
                this.user = data;
                this.username = data.txtUserName || '';
            }
        });
    }
}
