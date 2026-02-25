import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-receipt',
  templateUrl: './receipt.component.html',
  styleUrls: ['./receipt.component.css']
})
export class ReceiptComponent implements OnInit {
  orderData: any = null;

  constructor(private router: Router) {}

  ngOnInit(): void {
    const orderDataJson = sessionStorage.getItem('orderData');
    if (orderDataJson) {
      try {
        this.orderData = JSON.parse(orderDataJson);
      } catch (e) {
        console.error('Error parsing order data', e);
        this.router.navigate(['/product-catalog']);
      }
    } else {
      this.router.navigate(['/product-catalog']);
    }
  }

  printReceipt(): void {
    window.print();
  }

  goToFeedback(): void {
    this.router.navigate(['/feedback']);
  }

  continueShopping(): void {
    this.router.navigate(['/product-catalog']);
  }
}






