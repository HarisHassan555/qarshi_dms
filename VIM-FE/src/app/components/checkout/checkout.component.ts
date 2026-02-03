import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from 'src/app/services/cart/cart.service';
import { NotificationService } from 'src/app/NotificationService';

@Component({
  selector: 'app-checkout',
  templateUrl: './checkout.component.html',
  styleUrls: ['./checkout.component.css']
})
export class CheckoutComponent implements OnInit {
  checkoutForm!: FormGroup;
  cartItems: any[] = [];
  totalPrice: number = 0;
  totalItems: number = 0;
  isSubmitting: boolean = false;

  constructor(
    private fb: FormBuilder,
    private cartService: CartService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.cartItems = this.cartService.getCartItems();
    this.totalPrice = this.cartService.getTotalPrice();
    this.totalItems = this.cartService.getTotalItems();

    if (this.cartItems.length === 0) {
      this.notificationService.showMessage('Your cart is empty', 'warning');
      this.router.navigate(['/cart']);
      return;
    }

    this.initializeForm();
  }

  initializeForm(): void {
    // Get user from localStorage if available, otherwise use demo data
    const userJson = localStorage.getItem('user');
    let user: any = null;
    if (userJson) {
      try {
        user = JSON.parse(userJson);
      } catch (e) {
        console.error('Error parsing user data', e);
      }
    }

    // Demo data for testing if no user is logged in
    const demoData = {
      firstName: user?.txtFirstName || 'John',
      lastName: user?.txtLastName || 'Doe',
      email: user?.txtEmail || 'john.doe@example.com',
      phone: user?.txtPhone || '+1 (555) 123-4567',
      address: '123 Main Street',
      city: 'New York',
      state: 'NY',
      zipCode: '10001',
      country: 'United States',
      cardNumber: '4111111111111111',
      cardHolderName: 'John Doe',
      expiryDate: '12/25',
      cvv: '123'
    };

    this.checkoutForm = this.fb.group({
      firstName: [demoData.firstName, [Validators.required]],
      lastName: [demoData.lastName, [Validators.required]],
      email: [demoData.email, [Validators.required, Validators.email]],
      phone: [demoData.phone, [Validators.required]],
      address: [demoData.address, [Validators.required]],
      city: [demoData.city, [Validators.required]],
      state: [demoData.state, [Validators.required]],
      zipCode: [demoData.zipCode, [Validators.required]],
      country: [demoData.country, [Validators.required]],
      paymentMethod: ['credit-card', [Validators.required]],
      cardNumber: [demoData.cardNumber, [Validators.required, Validators.pattern(/^\d{16}$/)]],
      cardHolderName: [demoData.cardHolderName, [Validators.required]],
      expiryDate: [demoData.expiryDate, [Validators.required, Validators.pattern(/^\d{2}\/\d{2}$/)]],
      cvv: [demoData.cvv, [Validators.required, Validators.pattern(/^\d{3,4}$/)]]
    });
  }

  onSubmit(): void {
    if (this.checkoutForm.invalid) {
      this.markFormGroupTouched(this.checkoutForm);
      this.notificationService.showMessage('Please fill all required fields correctly', 'warning');
      return;
    }

    this.isSubmitting = true;

    // Simulate payment processing
    setTimeout(() => {
      const orderData = {
        ...this.checkoutForm.value,
        items: this.cartItems,
        totalPrice: this.totalPrice,
        totalItems: this.totalItems,
        orderDate: new Date().toISOString(),
        orderId: 'ORD-' + Date.now()
      };

      // Store order data for receipt
      sessionStorage.setItem('orderData', JSON.stringify(orderData));

      // Clear cart
      this.cartService.clearCart();

      // Navigate to receipt
      this.router.navigate(['/receipt']);
    }, 1500);
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/cart']);
  }
}

