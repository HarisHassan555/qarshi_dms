# Staging and Production API Configuration

## Overview
This document outlines the API endpoints and configurations for **Staging** and **Production** environments in the VIM (Vendor Invoice Management) application.

## Frontend API Configuration

### Current Configuration
**File**: `VIM-FE/src/app/utils/urls.ts`

```typescript
export class urls {
    static API_URL = 'http://localhost:8080/VIM/'  // Currently set to localhost
    static SIGNIN_URL = 'api/auth/login'
    static FORGOT_URL = 'auth/forgot'
    static COUNTRY_URL = 'country'
    static CITY_URL = 'city'
}
```

### Environment-Specific URLs

#### Staging Environment
```typescript
static API_URL = 'http://192.0.0.203:8080/VIM/'  // Staging server
```

#### Production Environment
```typescript
static API_URL = 'http://<production-ip>:8080/VIM/'  // Production server
```

**Note**: The production IP address needs to be confirmed with your infrastructure team.

## Backend API Configuration

### Application Context Path
**File**: `VIM-BE/src/main/resources/application.properties`

```properties
server.servlet.context-path=/VIM
```

This means all API endpoints are prefixed with `/VIM`, so:
- Base URL: `http://<server>:<port>/VIM`
- Login endpoint: `http://<server>:<port>/VIM/api/auth/login`

### Database Configuration

#### Current (Local Development)
```properties
spring.datasource.url=jdbc:mysql://localhost:3308/vim_3?useSSL=false&logAbandoned=true&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=dsg123
```

#### Staging Database
```properties
# Staging database configuration (to be configured)
spring.datasource.url=jdbc:mysql://<staging-db-host>:3308/vim_3?useSSL=false&logAbandoned=true&allowPublicKeyRetrieval=true
spring.datasource.username=<staging-db-user>
spring.datasource.password=<staging-db-password>
```

#### Production Database
```properties
# Production database configuration (to be configured)
spring.datasource.url=jdbc:mysql://<production-db-host>:3308/vim_3?useSSL=false&logAbandoned=true&allowPublicKeyRetrieval=true
spring.datasource.username=<production-db-user>
spring.datasource.password=<production-db-password>
```

## SOAP Service Endpoints

### Staging SOAP Endpoints (Currently Commented Out)

**File**: `VIM-BE/src/main/resources/application.properties`

#### Invoice Web Service (Staging)
```properties
#old(Staging)
#soap.endpoint.url=http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/110/zkf_e_invoice_web_service/zkf_e_invoice_web_service
#soap.username=MUJAHID_ABAP
#soap.password=dev110
```

#### SES Web Service (Staging)
```properties
#OLD (Staging)
#soap.endpoint.url.ses=http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_ses_web_service/110/zkf_e_invoice_ses_web_service/zkf_e_invoice_ses_web_service
```

#### Workflow Remarks Service (Staging)
```properties
#OLD (Staging)
#soap.endpoint.url.remarks=http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_invoice_workflow_remarks/110/zkf_invoice_workflow_remarks/zkf_invoice_workflow_remarks
```

### Production SOAP Endpoints (Currently Active)

#### Invoice Web Service (Production)
```properties
#new(PRD)
soap.endpoint.url=http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/300/zkf_e_invoice_web_service/zkf_e_invoice_web_service
soap.username=SAP-TMX
soap.password=SaP@ExD123
```

#### SES Web Service (Production)
```properties
#new(PRD)
soap.endpoint.url.ses=http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_ses_web_service/300/zkf_e_invoice_ses_web_service/zkf_e_invoice_ses_web_service
```

#### Workflow Remarks Service (Production)
```properties
#New(PRD)
soap.endpoint.url.remarks=http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_invoice_workflow_remarks/300/zkf_invoice_workflow_remarks/zkf_invoice_workflow_remarks
```

## SAP Server Configuration

**File**: `VIM-BE/src/main/java/com/bezkoder/spring/login/admin/ServerConfiguration.java`

### Current Configuration (Development)
```java
public static String ip_servre="http://192.168.0.27:8000";
public static boolean send_mail=false;
public static boolean integeration_required = false;
```

### Development Server (Commented)
```java
//	DEV server
//	public static String ip_servre="http://192.168.0.23:8000";
//	public static boolean send_mail=false;
```

### Production Server (Commented)
```java
//  192.168.0.20:8000 This is production server of ICL
//	public static String ip_servre="http://192.168.0.20:8000";   
//	public static boolean send_mail=true;
```

### External Services (SH - Service Hub)

#### Authentication Service
```java
public static String service_auth_tokenSH="http://115.167.64.214:5000/Auth/Login";
```

#### Customer Service
```java
public static String service_customerSH="http://115.167.64.214:5000/SCM/CreateCustomer";
```

#### Sales Order Service
```java
public static String service_SOSH="http://115.167.64.214:5000/SCM/CreateSalesOrder";
```

#### Payment Service
```java
public static String service_paymentSH="http://115.167.64.214:5000/SCM/CreateReceipt";
```

### QSA Server (SAP Integration)
```java
public static String service_customer="http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
public static String service_so="http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
public static String service_payment="http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
public static String service_cancel_so="http://vhgdids4ci.sap.gil.com.pk:8000/sap/bc/srt/rfc/sap/zbiafo_gnl_dms_int/120/zbiafo_gnl_dms_int/zbiafo_gnl_dms_int?sap-client=120";
public static String uname="dmsuser";
public static String Password="SAPdms@123";
```

## Environment Summary

### Staging Environment

| Component | URL/Configuration |
|-----------|-------------------|
| **Frontend API** | `http://192.0.0.203:8080/VIM/` |
| **Backend Context** | `/VIM` |
| **SOAP Invoice Service** | `http://192.0.2.151:8000/.../110/...` |
| **SOAP Username** | `MUJAHID_ABAP` |
| **SOAP Password** | `dev110` |
| **SAP Client** | `110` |
| **Database** | To be configured |

### Production Environment

| Component | URL/Configuration |
|-----------|-------------------|
| **Frontend API** | `http://<production-ip>:8080/VIM/` (TBD) |
| **Backend Context** | `/VIM` |
| **SOAP Invoice Service** | `http://192.0.2.150:8000/.../300/...` |
| **SOAP Username** | `SAP-TMX` |
| **SOAP Password** | `SaP@ExD123` |
| **SAP Client** | `300` |
| **SAP Server** | `http://192.168.0.20:8000` (commented) |
| **Database** | To be configured |

### Local Development Environment

| Component | URL/Configuration |
|-----------|-------------------|
| **Frontend API** | `http://localhost:8080/VIM/` |
| **Backend Context** | `/VIM` |
| **Database** | `localhost:3308/vim_3` |
| **SAP Server** | `http://192.168.0.27:8000` |

## Common API Endpoints

All endpoints are prefixed with `/VIM`:

### Authentication
- `POST /VIM/api/auth/login` - User login
- `POST /VIM/auth/forgot` - Password reset

### Sale Invoice
- `GET /VIM/getAllSaleOrder` - Get all sale orders
- `POST /VIM/searchSaleOrder` - Search sale orders
- `GET /VIM/sale-invoice` - Get sale invoice list (if exists)

### Reports
- `POST /VIM/getSaleInvoiceListReport` - Get sale invoice report

### Deal Management
- `POST /VIM/searchDeal` - Search deals
- `GET /VIM/getAllDealer` - Get all dealers

## Configuration Management

### Recommended Approach: Environment-Specific Properties Files

Create separate property files for each environment:

1. **`application-dev.properties`** - Local development
2. **`application-staging.properties`** - Staging environment
3. **`application-prod.properties`** - Production environment

Then use Spring profiles:
```bash
# Development
java -jar app.jar --spring.profiles.active=dev

# Staging
java -jar app.jar --spring.profiles.active=staging

# Production
java -jar app.jar --spring.profiles.active=prod
```

### Example Structure

**application-staging.properties**
```properties
spring.datasource.url=jdbc:mysql://staging-db-host:3308/vim_3?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=staging_user
spring.datasource.password=staging_password

soap.endpoint.url=http://192.0.2.151:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/110/zkf_e_invoice_web_service/zkf_e_invoice_web_service
soap.username=MUJAHID_ABAP
soap.password=dev110
```

**application-prod.properties**
```properties
spring.datasource.url=jdbc:mysql://prod-db-host:3308/vim_3?useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=prod_user
spring.datasource.password=prod_password

soap.endpoint.url=http://192.0.2.150:8000/sap/bc/srt/rfc/sap/zkf_e_invoice_web_service/300/zkf_e_invoice_web_service/zkf_e_invoice_web_service
soap.username=SAP-TMX
soap.password=SaP@ExD123
```

## Frontend Environment Configuration

### Recommended: Environment Files

Create environment files in `VIM-FE/src/environments/`:

**environment.ts** (default/development)
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/VIM/'
};
```

**environment.staging.ts**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://192.0.0.203:8080/VIM/'
};
```

**environment.prod.ts**
```typescript
export const environment = {
  production: true,
  apiUrl: 'http://<production-ip>:8080/VIM/'
};
```

Then update `urls.ts`:
```typescript
import { environment } from '../environments/environment';

export class urls {
    static API_URL = environment.apiUrl;
    static SIGNIN_URL = 'api/auth/login';
    static FORGOT_URL = 'auth/forgot';
    static COUNTRY_URL = 'country';
    static CITY_URL = 'city';
}
```

Build commands:
```bash
# Development
ng serve

# Staging
ng build --configuration=staging

# Production
ng build --configuration=production
```

## Security Notes

⚠️ **Important Security Considerations:**

1. **Never commit passwords or sensitive credentials** to version control
2. Use environment variables or secure vaults for production credentials
3. The current `application.properties` file contains hardcoded passwords - this should be changed
4. Consider using Spring Cloud Config or similar for centralized configuration management

## Next Steps

1. **Identify Production IP**: Confirm the production server IP address
2. **Create Environment Files**: Set up separate property files for each environment
3. **Update Frontend**: Implement environment-based configuration
4. **Secure Credentials**: Move sensitive data to environment variables or secure storage
5. **Documentation**: Update this document with actual production URLs once confirmed

## Contact

For production server details and database configurations, contact:
- Infrastructure/DevOps team
- Database administrator
- SAP integration team




