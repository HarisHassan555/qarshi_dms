import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CartService, CartItem } from 'src/app/services/cart/cart.service';
import { NotificationService } from 'src/app/NotificationService';

@Component({
  selector: 'app-cart',
  templateUrl: './cart.component.html',
  styleUrls: ['./cart.component.css']
})
export class CartComponent implements OnInit {
  cartItems: CartItem[] = [];
  totalPrice: number = 0;
  totalItems: number = 0;

  constructor(
    private cartService: CartService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadCart();
    
    // Subscribe to cart changes
    this.cartService.cart$.subscribe(items => {
      this.cartItems = items;
      this.calculateTotals();
    });
  }

  loadCart(): void {
    this.cartItems = this.cartService.getCartItems();
    this.calculateTotals();
  }

  calculateTotals(): void {
    this.totalPrice = this.cartService.getTotalPrice();
    this.totalItems = this.cartService.getTotalItems();
  }

  updateQuantity(item: CartItem, change: number): void {
    const newQuantity = item.quantity + change;
    if (newQuantity > 0) {
      this.cartService.updateQuantity(item.product.serProductId, newQuantity);
    } else {
      this.removeItem(item.product.serProductId);
    }
  }

  removeItem(productId: number): void {
    const product = this.cartItems.find(item => item.product.serProductId === productId)?.product;
    this.cartService.removeFromCart(productId);
    if (product) {
      this.notificationService.showMessage(
        `${product.txtProductName || 'Product'} removed from cart`,
        'info'
      );
    }
  }

  clearCart(): void {
    if (confirm('Are you sure you want to clear your cart?')) {
      this.cartService.clearCart();
      this.notificationService.showMessage('Cart cleared', 'info');
    }
  }

  proceedToCheckout(): void {
    if (this.cartItems.length === 0) {
      this.notificationService.showMessage('Your cart is empty', 'warning');
      return;
    }
    this.router.navigate(['/checkout']);
  }

  continueShopping(): void {
    this.router.navigate(['/product-catalog']);
  }
}





