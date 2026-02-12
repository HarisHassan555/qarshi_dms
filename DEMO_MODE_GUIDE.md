# Shopping Cart Demo Mode Guide

## 🎯 Demo Mode Features

The shopping cart feature now works completely in **demo mode** with dummy data, so you can test all functionality without backend integration.

## 📦 What's Included

### 1. **20 Sample Products**
- Electronics (Headphones, Webcam, Power Bank, etc.)
- Wearables (Smart Watch)
- Accessories (Keyboard, Mouse, Stands, etc.)
- Office Supplies (Desk Organizer, Chair, etc.)

### 2. **Pre-filled Checkout Form**
- All form fields are pre-filled with demo data
- You can modify any field
- Form validation still works
- Demo credit card: `4111111111111111` (valid test number)

### 3. **Auto-fallback to Demo Data**
- If API is not available, automatically uses dummy products
- Shows notification: "Demo mode: Using sample products"
- No errors, seamless experience

## 🚀 Quick Start

### Option 1: Use "Add Sample Items" Button
1. Go to `/product-catalog`
2. Click **"Add Sample Items"** button in the blue banner
3. This adds 3 random products to your cart
4. Proceed to checkout

### Option 2: Manual Selection
1. Browse products in `/product-catalog`
2. Click "Add to Cart" on any product
3. View cart at `/cart`
4. Proceed to checkout

## 📋 Demo Data Details

### Products
- **20 products** across 4 categories
- Prices range from $18.99 to $249.99
- All products have descriptions, codes, and categories
- Realistic product names and details

### Checkout Form (Pre-filled)
```
Name: John Doe
Email: john.doe@example.com
Phone: +1 (555) 123-4567
Address: 123 Main Street
City: New York
State: NY
Zip: 10001
Country: United States

Card: 4111111111111111
Name: John Doe
Expiry: 12/25
CVV: 123
```

## 🎨 Features That Work

✅ **Product Catalog**
- View all 20 products
- Search functionality
- Category filtering
- Add to cart

✅ **Shopping Cart**
- View cart items
- Update quantities
- Remove items
- Calculate totals

✅ **Checkout**
- Pre-filled form (editable)
- Form validation
- Order processing simulation

✅ **Receipt**
- Order confirmation
- Print functionality
- Order details display

✅ **Feedback**
- Star rating
- Comment submission
- Thank you message

## 💾 Data Persistence

- **Cart**: Saved in `localStorage` (persists across sessions)
- **Orders**: Saved in `sessionStorage` (cleared on browser close)
- **Feedback**: Saved in `localStorage` as 'feedbacks' array

## 🔄 How It Works

1. **Product Loading**:
   - First tries to load from API (`getActiveProducts()`)
   - If API fails or returns empty, uses dummy data
   - Shows notification about demo mode

2. **Cart Management**:
   - Uses `CartService` with localStorage
   - Works completely offline
   - Real-time updates

3. **Checkout**:
   - Pre-fills form with demo data
   - Simulates payment processing (1.5 second delay)
   - Creates order with unique ID

4. **Receipt & Feedback**:
   - Displays order from sessionStorage
   - Feedback saved locally
   - Thank you message with auto-redirect

## 🧪 Testing Scenarios

### Test Full Flow
1. Go to `/product-catalog`
2. Click "Add Sample Items"
3. Go to `/cart` - see 3 items
4. Click "Proceed to Checkout"
5. Form is pre-filled - click "Complete Order"
6. View receipt at `/receipt`
7. Click "Provide Feedback"
8. Rate and comment, submit
9. See thank you message

### Test Individual Features
- **Search**: Type "headphone" or "keyboard"
- **Filter**: Select "Electronics" category
- **Cart Operations**: Add, remove, update quantities
- **Form Validation**: Try submitting empty form
- **Print**: Click print on receipt

## 📝 Notes

- All data is stored locally (no backend required)
- Cart persists even after browser refresh
- Orders are stored in sessionStorage (cleared on close)
- Feedback is saved in localStorage
- Demo mode notification appears when using dummy data

## 🔧 Customization

To add more products, edit:
`VIM-FE/src/app/services/cart/dummy-products.service.ts`

To change demo checkout data, edit:
`VIM-FE/src/app/components/checkout/checkout.component.ts` (initializeForm method)

---

**Enjoy testing the shopping cart feature!** 🛒




