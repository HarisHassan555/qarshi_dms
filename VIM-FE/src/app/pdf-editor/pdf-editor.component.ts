import { Component } from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import { PDFDocument, StandardFonts, rgb, degrees } from 'pdf-lib'
import {SaleOrderService} from "../services/saleorder/sale-order.service";
import {NotificationService} from "../NotificationService";
import {DocumentService} from "../services/document/document-service";

@Component({
  selector: 'app-pdf-editor',
  templateUrl: './pdf-editor.component.html',
  styleUrls: ['./pdf-editor.component.css']
})
export class PdfEditorComponent {

    pdfBytes: Uint8Array | undefined;
    documentId: string | null | undefined;
    // @ts-ignore
    imageOffsets = JSON.parse(sessionStorage.getItem('imageOffsets')) || [];
    isUserHead: boolean = false;
    constructor(private route: ActivatedRoute,private saleOrderService: SaleOrderService, private notificationService: NotificationService,private documentService: DocumentService) { }


    onFileSelected(event: Event) {
        const fileInput = event.target as HTMLInputElement;

        if (fileInput.files && fileInput.files[0]) {
            const file = fileInput.files[0];
            const reader = new FileReader();

            reader.onload = (e) => {
                const imageBytes = e.target?.result;
                console.log(imageBytes);
            };

            reader.readAsArrayBuffer(file);
        }
    }

    ngOnInit(): void {
        this.documentId = this.route.snapshot.paramMap.get('documentId');
        //alert(this.documentId)
        const userJson = localStorage.getItem('user');
        if (userJson) {
            // @ts-ignore
            const user = JSON.parse(userJson) as CfgTblUser;

            // Set 'isUserHead' to true if the user's role is a head role
            const headRoles = ['MARKETING_HEAD', 'PROCUREMENT_HEAD', 'TAX_HEAD', 'FINANCE_HEAD', 'AUDIT_HEAD', 'PAYMENT_HEAD'];
            this.isUserHead = headRoles.includes(user.cfgTblRole?.txtRoleName);
        }
        this.loadPdf(this.documentId);

    }


    async loadPdf(documentId: string | null | undefined) {
        if (!documentId) {
            this.notificationService.showMessage('Document ID is required', 'danger');
            return;
        }

        try {
            // Fetch the document from the service
            const data = await this.documentService.downloadDocument(documentId).toPromise();

            // Create a Blob from the fetched data
            // @ts-ignore
            const blob = new Blob([data], { type: 'application/pdf' });
            const objectUrl = URL.createObjectURL(blob);

            // Open the PDF in a new tab
           // window.open(objectUrl, '_blank');

            // Optionally, you can update the PDF viewer if needed
            this.pdfBytes = new Uint8Array(await blob.arrayBuffer());
            this.updatePdfViewer();
        } catch (error) {
            this.notificationService.showMessage('Unable to view Document...', 'danger');
            console.error('Error loading PDF:', error);
        }
    }

    /*async loadPdf(documentId: string | null | undefined) {
        // @ts-ignore
        this.documentService.downloadDocument(documentId).subscribe(
            (data) => {
                const blob = new Blob([data], { type: 'application/pdf' });
                const objectUrl = window.URL.createObjectURL(blob);
                window.open(objectUrl, '_blank');
            },
            () => {
                this.notificationService.showMessage('Unable to view Document...','danger');
            }
        );
        const blob = new Blob([this.pdfBytes], { type: 'application/pdf' });
       /!* const url = 'https://pdf-lib.js.org/assets/with_update_sections.pdf';*!/
        const existingPdfBytes = await fetch(url).then(res => res.arrayBuffer());
        this.pdfBytes = new Uint8Array(existingPdfBytes);
        this.updatePdfViewer();
    }*/

    updatePdfViewer() {
        // @ts-ignore
        const blob = new Blob([this.pdfBytes], { type: 'application/pdf' });
        const urlBlob = URL.createObjectURL(blob);
        const pdfViewer = document.getElementById('pdf-viewer') as HTMLIFrameElement;
        pdfViewer.src = urlBlob;
    }

    async addImageToPdf(file: File) {
        // @ts-ignore
        const pdfDoc = await PDFDocument.load(this.pdfBytes);
        const imageBytes = await file.arrayBuffer();
        const image = await pdfDoc.embedJpg(imageBytes);
        const imageDims = image.scale(0.3);

        const pages = pdfDoc.getPages();
        const lastPage = pages[pages.length - 1];
        const xPosition = lastPage.getWidth() - imageDims.width - 30;

        // Determine the next yPosition
        let yPosition = 50;

        // If there are existing images, calculate the next position
        if (this.imageOffsets.length > 0) {
            const lastOffset = this.imageOffsets[this.imageOffsets.length - 1];
            yPosition = lastOffset + imageDims.height + 10; // Add some space between images
        }

        // Draw the new image
        lastPage.drawImage(image, {
            x: xPosition,
            y: yPosition,
            width: imageDims.width,
            height: imageDims.height,
        });

        // Save the new yPosition to offsets and update session storage
        this.imageOffsets.push(yPosition);
        sessionStorage.setItem('imageOffsets', JSON.stringify(this.imageOffsets));

        this.pdfBytes = await pdfDoc.save();
        this.updatePdfViewer();
    }

    /*async addImageToPdf(file: File) {
        // @ts-ignore
        const pdfDoc = await PDFDocument.load(this.pdfBytes);
        const imageBytes = await file.arrayBuffer();
        const image = await pdfDoc.embedJpg(imageBytes);
        const imageDims = image.scale(0.3);

        const pages = pdfDoc.getPages();
        const lastPage = pages[pages.length - 1];
        const xPosition = lastPage.getWidth() - imageDims.width - 30;
        const yPosition = 50;

        lastPage.drawImage(image, {
            x: xPosition,
            y: yPosition,
            width: imageDims.width,
            height: imageDims.height,
        });

        this.pdfBytes = await pdfDoc.save();
        this.updatePdfViewer();
    }*/

    /*async savePdf() {
        // @ts-ignore
        const blob = new Blob([this.pdfBytes], { type: 'application/pdf' });
        const formData = new FormData();
        formData.append('pdf', blob, 'modified.pdf');
        // @ts-ignore
        formData.append('fileId', this.documentId); // Add the file ID

        const response = await fetch('http://localhost:3000/upload', {
            method: 'POST',
            body: formData,
        });

        const result = await response.json();
        if (response.ok) {
            alert(`PDF saved successfully: ${result.filename}`);
        } else {
            alert('Error saving PDF');
        }
    }*/

    onAddImageClick() {
       // const imageUrl = (document.getElementById('image-url') as HTMLInputElement).value;
       /* const input = document.getElementById('image-url');*/
        const userJson = localStorage.getItem('user');
        // @ts-ignore
        let user: {
            cfgTblRole: number | undefined;
            serUserId: number; };
        if (userJson) {
            // @ts-ignore
            user = JSON.parse(userJson) as CfgTblUser;
        }
        // @ts-ignore
        //this.loadPermissionRoles(user.cfgTblRole.serRoleId,user.serUserId);
        // @ts-ignore
       // this.isHead = user.cfgTblRole?.txtRoleName === 'MARKETING_HEAD';
        let imagePath: string;
        // @ts-ignore
        switch (user.cfgTblRole?.txtRoleName) {
            case 'MARKETING_HEAD':
                imagePath = 'assets/images/MARKETING_HEAD.jpg';
                break;
            case 'PROCUREMENT_HEAD':
                imagePath = 'assets/images/PROCUREMENT_HEAD.jpg';
                break;
            case 'TAX_HEAD':
                imagePath = 'assets/images/TAX_HEAD.jpg';
                break;
            case 'FINANCE_HEAD':
                imagePath = 'assets/images/FINANCE_HEAD.jpg';
                break;
            case 'AUDIT_HEAD':
                imagePath = 'assets/images/AUDIT_HEAD.jpg';
                break;
            case 'PAYMENT_HEAD':
                imagePath = 'assets/images/PAYMENT_HEAD.jpg';
                break;
            default:
                this.notificationService.showMessage('Unknown user role', 'danger');
                return; // Exit if no valid role is found
        }

       // console.log(this.items)
        // @ts-ignore
        this.convertImageToBlob(imagePath).then(blob => {
            // Create a File object from the Blob (you can specify the name of the file here)
            // @ts-ignore
            const file = new File([blob], imagePath.split('/').pop(), { type: blob.type });

            // Add the Blob image directly to the PDF
            this.addImageToPdf(file);  // Add the Blob image to the PDF
        }).catch(error => {
            this.notificationService.showMessage('Failed to load the image.', 'danger');
            console.error('Error converting image to Blob:', error);
        });
       /* if (input.files.length > 0) {
            // @ts-ignore
            const file = input.files[0];
            // @ts-ignore
            this.addImageToPdf(file);
        } else {
           /!* alert("Please enter a valid image URL.");*!/
            this.notificationService.showMessage('Please enter a valid image URL.','danger');
        }*/
    }

    onSavePdfClick() {
        if (this.pdfBytes) {
           /* this.savePdf();*/
            // @ts-ignore
            this.saleOrderService
                .savePdf(this.pdfBytes,this.documentId);
            this.notificationService.showMessage('File saved successfully','success');
                /*.subscribe((response: any) => {
                    if (response) {
                        if (response === 'Success') {
                            this.notificationService.showMessage('File saved successfully','success');
                            // alert(`PDF saved successfully: ${result.filename}`);
                        } else {
                            // alert('Error saving PDF');
                            this.notificationService.showMessage('Error saving PDF','danger');
                        }
                    }
                });*/
        } else {
            this.notificationService.showMessage('No PDF to save','danger');
        }
    }


    convertImageToBlob(imagePath: string): Promise<Blob> {
        return fetch(imagePath)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to fetch image');
                }
                return response.blob();
            });
    }

}
