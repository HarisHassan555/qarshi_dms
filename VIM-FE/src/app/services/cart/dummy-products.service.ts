import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class DummyProductsService {
  
  getDummyProducts(): any[] {
    return [
      {
        serProductId: 1,
        txtProductCode: 'PROD-001',
        txtProductName: 'Wireless Bluetooth Headphones',
        txtDescription: 'Premium quality wireless headphones with noise cancellation and 30-hour battery life. Perfect for music lovers and professionals.',
        decPrice: 79.99,
        price: 79.99,
        txtSapCode: 'SAP-001',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 1,
          txtProductCategoryName: 'Electronics'
        }
      },
      {
        serProductId: 2,
        txtProductCode: 'PROD-002',
        txtProductName: 'Smart Watch Pro',
        txtDescription: 'Feature-rich smartwatch with heart rate monitor, GPS, and water resistance. Track your fitness goals with style.',
        decPrice: 199.99,
        price: 199.99,
        txtSapCode: 'SAP-002',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 2,
          txtProductCategoryName: 'Wearables'
        }
      },
      {
        serProductId: 3,
        txtProductCode: 'PROD-003',
        txtProductName: 'Laptop Stand Adjustable',
        txtDescription: 'Ergonomic aluminum laptop stand with adjustable height and angle. Improve your workspace setup and reduce neck strain.',
        decPrice: 49.99,
        price: 49.99,
        txtSapCode: 'SAP-003',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 4,
        txtProductCode: 'PROD-004',
        txtProductName: 'Mechanical Keyboard RGB',
        txtDescription: 'Gaming mechanical keyboard with RGB backlighting, Cherry MX switches, and programmable keys. Perfect for gamers and typists.',
        decPrice: 129.99,
        price: 129.99,
        txtSapCode: 'SAP-004',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 5,
        txtProductCode: 'PROD-005',
        txtProductName: 'USB-C Hub 7-in-1',
        txtDescription: 'Multi-port USB-C hub with HDMI, USB 3.0, SD card reader, and power delivery. Expand your laptop connectivity.',
        decPrice: 39.99,
        price: 39.99,
        txtSapCode: 'SAP-005',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 6,
        txtProductCode: 'PROD-006',
        txtProductName: 'Wireless Mouse Ergonomic',
        txtDescription: 'Comfortable wireless mouse with ergonomic design, long battery life, and precise tracking. Ideal for office and home use.',
        decPrice: 29.99,
        price: 29.99,
        txtSapCode: 'SAP-006',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 7,
        txtProductCode: 'PROD-007',
        txtProductName: '4K Webcam HD',
        txtDescription: 'Ultra HD 4K webcam with auto-focus, noise reduction, and privacy shutter. Perfect for video calls and streaming.',
        decPrice: 89.99,
        price: 89.99,
        txtSapCode: 'SAP-007',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 1,
          txtProductCategoryName: 'Electronics'
        }
      },
      {
        serProductId: 8,
        txtProductCode: 'PROD-008',
        txtProductName: 'Portable Power Bank 20000mAh',
        txtDescription: 'High-capacity power bank with fast charging, USB-C and USB-A ports. Keep your devices powered on the go.',
        decPrice: 34.99,
        price: 34.99,
        txtSapCode: 'SAP-008',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 1,
          txtProductCategoryName: 'Electronics'
        }
      },
      {
        serProductId: 9,
        txtProductCode: 'PROD-009',
        txtProductName: 'Desk Organizer Set',
        txtDescription: 'Premium desk organizer with multiple compartments for pens, papers, and office supplies. Keep your workspace tidy.',
        decPrice: 24.99,
        price: 24.99,
        txtSapCode: 'SAP-009',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 4,
          txtProductCategoryName: 'Office Supplies'
        }
      },
      {
        serProductId: 10,
        txtProductCode: 'PROD-010',
        txtProductName: 'Monitor Stand with Storage',
        txtDescription: 'Sleek monitor stand with built-in storage compartments. Elevate your monitor and organize your desk space.',
        decPrice: 59.99,
        price: 59.99,
        txtSapCode: 'SAP-010',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 11,
        txtProductCode: 'PROD-011',
        txtProductName: 'Cable Management Kit',
        txtDescription: 'Complete cable management solution with clips, ties, and sleeves. Organize and hide cables for a clean setup.',
        decPrice: 19.99,
        price: 19.99,
        txtSapCode: 'SAP-011',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 12,
        txtProductCode: 'PROD-012',
        txtProductName: 'LED Desk Lamp USB',
        txtDescription: 'Modern LED desk lamp with adjustable brightness, color temperature control, and USB charging port.',
        decPrice: 44.99,
        price: 44.99,
        txtSapCode: 'SAP-012',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 13,
        txtProductCode: 'PROD-013',
        txtProductName: 'Laptop Sleeve Protective',
        txtDescription: 'Durable laptop sleeve with padding and water-resistant material. Protect your laptop during transport.',
        decPrice: 27.99,
        price: 27.99,
        txtSapCode: 'SAP-013',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 14,
        txtProductCode: 'PROD-014',
        txtProductName: 'Wireless Charging Pad',
        txtDescription: 'Fast wireless charging pad compatible with Qi-enabled devices. Charge your phone without cables.',
        decPrice: 22.99,
        price: 22.99,
        txtSapCode: 'SAP-014',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 1,
          txtProductCategoryName: 'Electronics'
        }
      },
      {
        serProductId: 15,
        txtProductCode: 'PROD-015',
        txtProductName: 'Noise Cancelling Earbuds',
        txtDescription: 'Premium wireless earbuds with active noise cancellation, 8-hour battery, and crystal-clear sound quality.',
        decPrice: 119.99,
        price: 119.99,
        txtSapCode: 'SAP-015',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 1,
          txtProductCategoryName: 'Electronics'
        }
      },
      {
        serProductId: 16,
        txtProductCode: 'PROD-016',
        txtProductName: 'Standing Desk Converter',
        txtDescription: 'Adjustable standing desk converter that transforms any desk into a sit-stand workstation. Improve your health and productivity.',
        decPrice: 179.99,
        price: 179.99,
        txtSapCode: 'SAP-016',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 17,
        txtProductCode: 'PROD-017',
        txtProductName: 'Ergonomic Office Chair',
        txtDescription: 'Comfortable ergonomic office chair with lumbar support, adjustable height, and 360-degree swivel. Support your back during long work hours.',
        decPrice: 249.99,
        price: 249.99,
        txtSapCode: 'SAP-017',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 4,
          txtProductCategoryName: 'Office Supplies'
        }
      },
      {
        serProductId: 18,
        txtProductCode: 'PROD-018',
        txtProductName: 'Tablet Stand Adjustable',
        txtDescription: 'Versatile tablet stand with adjustable angles and height. Perfect for reading, video calls, or as a second screen.',
        decPrice: 34.99,
        price: 34.99,
        txtSapCode: 'SAP-018',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 3,
          txtProductCategoryName: 'Accessories'
        }
      },
      {
        serProductId: 19,
        txtProductCode: 'PROD-019',
        txtProductName: 'USB Flash Drive 128GB',
        txtDescription: 'High-speed USB 3.0 flash drive with 128GB storage capacity. Reliable and portable data storage solution.',
        decPrice: 18.99,
        price: 18.99,
        txtSapCode: 'SAP-019',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 1,
          txtProductCategoryName: 'Electronics'
        }
      },
      {
        serProductId: 20,
        txtProductCode: 'PROD-020',
        txtProductName: 'Desk Mat Large',
        txtDescription: 'Premium large desk mat with smooth surface for mouse tracking and keyboard placement. Protect your desk and enhance your workspace.',
        decPrice: 39.99,
        price: 39.99,
        txtSapCode: 'SAP-020',
        blnStatus: true,
        cfgTblProductCategory: {
          serProductCategoryId: 4,
          txtProductCategoryName: 'Office Supplies'
        }
      }
    ];
  }
}



