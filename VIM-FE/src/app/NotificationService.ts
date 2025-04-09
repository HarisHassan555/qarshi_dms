import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

@Injectable({
    providedIn: 'root'
})
export class NotificationService {

    constructor() { }


    showMessage(msg = '', type = '') {
        // @ts-ignore
        // @ts-ignore
        const toast: any = Swal.mixin({
            toast: true,
            position: 'top-right',
            showConfirmButton: false,
            timer: 8000,
            showCloseButton: true,
            customClass: {
                popup: type === 'success' ? 'color-success' : 'color-danger'
            },
        });
        toast.fire({
            icon: type,
            title: msg,
            padding: '10px 20px',
        });
    }
    /*showMessage(
        msg: string = 'Example notification text.',
        position: 'top-start' | 'top-end' | 'bottom-start' | 'bottom-end' = 'bottom-start',
        showCloseButton: boolean = true,
        closeButtonHtml: string = '',
        duration: number = 3000
    ): void {
        const toast = Swal.mixin({
            toast: true,
            position: position,
            showConfirmButton: false,
            timer: duration,
            showCloseButton: showCloseButton,
            customClass: {
                closeButton: closeButtonHtml ? closeButtonHtml : ''
            }
        });

        toast.fire({
            title: msg,
        });
    }*/
}
