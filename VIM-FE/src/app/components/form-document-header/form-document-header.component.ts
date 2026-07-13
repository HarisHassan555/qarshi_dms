import { Component, Input } from '@angular/core';
import { FormControl } from '@angular/forms';

@Component({
  selector: 'app-form-document-header',
  templateUrl: './form-document-header.component.html',
  styleUrls: ['./form-document-header.component.css']
})
export class FormDocumentHeaderComponent {
  @Input() headingControl: FormControl | null = null;
  @Input() headingValue: string = '';
  @Input() headingLabel: string = 'Form Heading';
  @Input() placeholder: string = 'Enter form heading';
  @Input() required: boolean = false;
  @Input() readonly: boolean = false;
  @Input() dateText: string = '';
  @Input() logoPath: string = 'assets/images/qarshi-logo.png';
  @Input() brandTitle: string = 'Qarshi Industries (Pvt) Ltd.';
  @Input() brandAddress: string = '15-G, Jam-e-Shirin Boulevard, Gulberg-III, Lahore';

  private readonly defaultDate = new Date().toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).replace(/ /g, '-');

  get displayDate(): string {
    return this.dateText || this.defaultDate;
  }
}

