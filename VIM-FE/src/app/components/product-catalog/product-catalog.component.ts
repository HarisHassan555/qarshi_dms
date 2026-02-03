import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ProductService } from 'src/app/services/product/product.service';
import { CartService } from 'src/app/services/cart/cart.service';
import { NotificationService } from 'src/app/NotificationService';
import { DummyProductsService } from 'src/app/services/cart/dummy-products.service';

@Component({
  selector: 'app-product-catalog',
  templateUrl: './product-catalog.component.html',
  styleUrls: ['./product-catalog.component.css']
})
export class ProductCatalogComponent implements OnInit {
  products: any[] = [];
  filteredProducts: any[] = [];
  searchTerm: string = '';
  selectedCategory: string = '';
  categories: any[] = [];
  loading: boolean = false;
  cartItemCount: number = 0;

  constructor(
    private productService: ProductService,
    private cartService: CartService,
    private router: Router,
    private notificationService: NotificationService,
    private dummyProductsService: DummyProductsService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.updateCartCount();
    
    // Subscribe to cart changes
    this.cartService.cart$.subscribe(() => {
      this.updateCartCount();
    });
  }

  loadProducts(): void {
    this.loading = true;
    // Try to load from API first, fallback to dummy data
    this.productService.getActiveProducts().subscribe(
      (data: any) => {
        if (data && Array.isArray(data) && data.length > 0) {
          this.products = data;
          this.filteredProducts = data;
          // Extract unique categories
          this.categories = [...new Set(data.map((p: any) => 
            p.cfgTblProductCategory?.txtProductCategoryName || 'Uncategorized'
          ))];
        } else {
          // Use dummy data if API returns empty or no data
          this.loadDummyProducts();
        }
        this.loading = false;
      },
      (error) => {
        console.warn('API not available, using dummy data:', error);
        // Use dummy data on error
        this.loadDummyProducts();
        this.loading = false;
      }
    );
  }

  loadDummyProducts(): void {
    this.products = this.dummyProductsService.getDummyProducts();
    this.filteredProducts = this.products;
    // Extract unique categories
    this.categories = [...new Set(this.products.map((p: any) => 
      p.cfgTblProductCategory?.txtProductCategoryName || 'Uncategorized'
    ))];
    this.notificationService.showMessage('Demo mode: Using sample products', 'info');
  }

  addToCart(product: any): void {
    this.cartService.addToCart(product, 1);
    this.notificationService.showMessage(
      `${product.txtProductName || 'Product'} added to cart!`,
      'success'
    );
    this.updateCartCount();
  }

  updateCartCount(): void {
    this.cartItemCount = this.cartService.getTotalItems();
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  onSearch(): void {
    this.filterProducts();
  }

  onCategoryChange(): void {
    this.filterProducts();
  }

  filterProducts(): void {
    this.filteredProducts = this.products.filter(product => {
      const matchesSearch = !this.searchTerm || 
        (product.txtProductName?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
         product.txtProductCode?.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
         product.txtDescription?.toLowerCase().includes(this.searchTerm.toLowerCase()));
      
      const matchesCategory = !this.selectedCategory || 
        (product.cfgTblProductCategory?.txtProductCategoryName === this.selectedCategory);
      
      return matchesSearch && matchesCategory;
    });
  }

  clearFilters(): void {
    this.searchTerm = '';
    this.selectedCategory = '';
    this.filterProducts();
  }

  addSampleToCart(): void {
    // Add a few sample products to cart for demo
    const sampleProducts = this.products.slice(0, 3);
    sampleProducts.forEach(product => {
      this.cartService.addToCart(product, Math.floor(Math.random() * 3) + 1);
    });
    this.notificationService.showMessage('Sample products added to cart!', 'success');
    this.updateCartCount();
  }
}

