# VIM (Vendor Invoice Management) - Full Stack Architecture

## 📋 Overview
**VIM** is a Vendor Invoice Management System built with a modern three-tier architecture:
- **Frontend**: Angular 15 with TypeScript
- **Backend**: Spring Boot 2.7.6 with Java 17
- **Database**: MySQL 8

---

## 🎨 Frontend Stack (VIM-FE)

### Core Framework
- **Angular**: `^15.2.0`
- **TypeScript**: `~4.9.4`
- **Node.js**: Required for development

### Key Dependencies

#### UI/UX Libraries
- **Tailwind CSS**: `^3.4.1` - Utility-first CSS framework
- **ApexCharts**: `^3.37.3` - Chart library
- **ng-apexcharts**: `^1.7.4` - Angular wrapper for ApexCharts
- **Swiper**: `^9.2.0` - Touch slider
- **FullCalendar**: `^6.1.8` - Calendar component
- **SweetAlert2**: `^11.7.3` - Beautiful alerts/modals
- **ngx-tippy-wrapper**: `^6.1.0` - Tooltips

#### Form & Input Libraries
- **@angular/forms**: `^15.2.0` - Angular forms
- **@ng-select/ng-select**: `^10.0.4` - Select dropdown component
- **ng2-flatpickr**: `^9.0.0` - Date picker
- **angular2-text-mask**: `^9.0.0` - Input masking
- **ngx-quill**: `^21.0.0` - Rich text editor
- **ngx-number-spinner**: `^2.2.1` - Number spinner

#### Data & State Management
- **@ngrx/store**: `^15.4.0` - State management
- **rxjs**: `~7.8.0` - Reactive programming
- **@bhplugin/ng-datatable**: `^0.0.2` - Data tables

#### File & Export Libraries
- **file-saver**: `^2.0.5` - File download
- **xlsx**: `^0.18.5` - Excel file handling
- **ang-json2excel-btn**: `^2.0.2` - JSON to Excel converter
- **pdf-lib**: `^1.17.1` - PDF manipulation
- **html2pdf.js**: `^0.14.0` - HTML to PDF conversion

#### Internationalization
- **@ngx-translate/core**: `^14.0.0` - i18n support
- **@ngx-translate/http-loader**: `^7.0.0` - HTTP loader for translations
- Supports: English, Arabic, Spanish, French, German, Italian, Japanese, Chinese, and more

#### Utilities
- **moment**: `^2.30.1` - Date/time manipulation
- **ngx-clipboard**: `^16.0.0` - Clipboard functionality
- **sortablejs**: `^1.15.0` - Drag and drop sorting

### Development Tools
- **@angular/cli**: `~15.2.4`
- **Karma**: `~6.4.0` - Test runner
- **Jasmine**: `~4.5.0` - Testing framework
- **Prettier**: `^2.8.7` - Code formatter

### Build Configuration
- **Output Path**: `dist/`
- **Base Href**: `/`
- **Production Build**: Optimized with code splitting
- **Development Server**: `ng serve` (default port 4200)

---

## ⚙️ Backend Stack (VIM-BE)

### Core Framework
- **Spring Boot**: `2.7.6`
- **Java**: `17`
- **Maven**: Build tool
- **Packaging**: WAR (deployed to external servlet container)

### Spring Modules

#### Core Spring Boot Starters
- **spring-boot-starter-web**: REST API support
- **spring-boot-starter-data-jpa**: JPA/Hibernate ORM
- **spring-boot-starter-security**: Security framework
- **spring-boot-starter-validation**: Bean validation
- **spring-boot-starter-undertow**: Undertow web server (instead of Tomcat)

#### Security & Authentication
- **JWT (JSON Web Tokens)**: `jjwt 0.11.5`
  - `jjwt-api`
  - `jjwt-impl`
  - `jjwt-jackson`
- **BCrypt**: Password hashing
- **Spring Security**: Method-level security enabled

### Database & ORM
- **MySQL Connector**: `mysql-connector-j`
- **Hibernate**: JPA implementation
- **Hibernate Dialect**: `MySQL8Dialect`
- **Connection Pool**: HikariCP
  - Max Pool Size: 50
  - Min Idle: 10
  - Connection Timeout: 30s

### External Integrations

#### SOAP Web Services
- **SAP Integration**: Multiple SOAP endpoints
  - E-Invoice Web Service
  - SES Web Service
  - Workflow Remarks Service
- **JAX-WS**: SOAP client implementation
- **saaj-impl**: `1.5.2` - SOAP with Attachments API

#### Email Service
- **JavaMail API**: `1.5.5`
- **SMTP**: Gmail SMTP server
- **TLS**: Enabled for secure email

#### HTTP Client
- **Unirest**: `1.4.9` - HTTP client library

### File Processing
- **Apache POI**: `5.2.3`
  - `poi` - Excel file processing
  - `poi-ooxml` - Office Open XML support
- **OpenCSV**: `5.9` - CSV file processing

### JSON & Serialization
- **Jackson**: JSON processing
  - `jackson-databind`
  - `jackson-datatype-hibernate5` - Hibernate lazy loading support

### Utilities
- **Joda-Time**: `2.10.14` - Date/time utilities
- **Log4j**: `1.2.17` - Logging framework

### Application Configuration
- **Context Path**: `/VIM`
- **JWT Secret**: Configured in `application.properties`
- **JWT Expiration**: 86400000ms (24 hours)
- **File Upload**: Max 35MB per file/request
- **CORS**: Configured for frontend communication

### Scheduled Jobs
- **Cron Jobs**: Configured for automated tasks
  - SO Job: `0 30 13 * * *` (1:30 PM daily)
  - Department Job: `0 59 13 * * *` (1:59 PM daily)

---

## 🗄️ Database Stack

### Database System
- **MySQL**: Version 8
- **Port**: 3308 (default 3306)
- **Database Name**: `vim_3`
- **Storage Engine**: MyISAM (configured)

### Database Configuration
```properties
spring.datasource.url=jdbc:mysql://localhost:3308/vim_3
spring.datasource.username=root
spring.jpa.hibernate.ddl-auto=update
```

### Key Database Features
- **Auto-increment**: Configured for primary keys
- **Foreign Keys**: Referential integrity
- **Indexes**: Performance optimization
- **Transactions**: ACID compliance

### Database Schema Areas
Based on SQL files and entity structure:

1. **User Management**
   - Users, Roles, Permissions
   - Authentication & Authorization

2. **Master Data**
   - Customers, Products, Accounts
   - Departments, Designations
   - Tax, Payment Terms

3. **Application Management**
   - Custom Form Applications
   - Approval Pipelines
   - Workflow History

4. **Deal Management**
   - Deals, Sales Orders
   - Distribution Channels
   - Product Categories

5. **Menu & Navigation**
   - Menu Structure
   - Sub-menu Roles
   - Department Permissions

---

## 🏗️ Architecture Overview

### Backend Architecture (Layered)

```
┌─────────────────────────────────────┐
│      Controllers (REST API)         │
│  - AuthController                   │
│  - UserController                   │
│  - DealController                   │
│  - CustomFormController             │
│  - 40+ Controllers                  │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Service Layer (BLL)            │
│  - Business Logic                   │
│  - Transaction Management           │
│  - Email Service                    │
│  - SOAP Client Service              │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Data Access Layer (DAL)        │
│  - DAO Interfaces                   │
│  - DAO Implementations              │
│  - Entity Models                    │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Database (MySQL)               │
└─────────────────────────────────────┘
```

### Security Architecture

```
┌─────────────────────────────────────┐
│      Frontend (Angular)             │
│  - JWT Token Storage                │
│  - HTTP Interceptors                │
└─────────────────────────────────────┘
              ↓ HTTP Request
              ↓ (JWT Token in Header)
┌─────────────────────────────────────┐
│      Spring Security Filter Chain   │
│  - AuthTokenFilter                  │
│  - JWT Validation                   │
│  - Authentication Provider          │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Controllers                    │
│  - @PreAuthorize                    │
│  - Role-based Access                │
└─────────────────────────────────────┘
```

### Frontend Architecture

```
┌─────────────────────────────────────┐
│      Components                     │
│  - 155+ HTML Templates              │
│  - 391+ TypeScript Components       │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      Services                       │
│  - HTTP Services                    │
│  - State Management (NgRx)         │
│  - Translation Service              │
└─────────────────────────────────────┘
              ↓
┌─────────────────────────────────────┐
│      REST API (Spring Boot)         │
└─────────────────────────────────────┘
```

---

## 🔌 API Communication

### REST API Endpoints
- **Base URL**: `http://localhost:8080/VIM`
- **Authentication**: JWT Bearer Token
- **Content-Type**: `application/json`
- **CORS**: Enabled for Angular frontend

### Key API Categories
1. **Authentication** (`/api/auth/**`)
   - Login, Signup, Token Refresh

2. **User Management** (`/api/user/**`)
   - User CRUD operations
   - Role management

3. **Master Data** (`/api/master/**`)
   - Customers, Products, Accounts
   - Departments, Designations

4. **Application Management** (`/api/application/**`)
   - Custom forms
   - Approval workflows

5. **Deal Management** (`/api/deal/**`)
   - Deals, Sales Orders
   - KPIs and Reports

---

## 🔐 Security Features

### Authentication
- **JWT-based**: Stateless authentication
- **Token Expiration**: 24 hours
- **Password Encryption**: BCrypt hashing
- **Session Management**: Stateless (JWT only)

### Authorization
- **Role-based Access Control (RBAC)**
- **Method-level Security**: `@PreAuthorize`
- **URL-based Permissions**: Configured in Security Filter Chain
- **Department-based Access**: Multi-tenant support

### Security Configuration
- **CSRF**: Disabled (JWT-based)
- **CORS**: Configured for frontend origin
- **Public Endpoints**: 
  - `/api/auth/**`
  - `/api/test/**`
  - `/login`, `/allMenu`
  - Email approval endpoints

---

## 📦 Deployment

### Backend Deployment
- **Format**: WAR file
- **Container**: External servlet container (Tomcat/Undertow)
- **Build Command**: `mvn clean package`
- **Output**: `target/VIM.war`

### Frontend Deployment
- **Format**: Static files
- **Build Command**: `ng build --configuration production`
- **Output**: `dist/` directory
- **Web Server**: Nginx, Apache, or any static file server

### Database Setup
- **MySQL**: Version 8 required
- **Initialization**: SQL scripts in `VIM-BE/` directory
- **Schema Updates**: Hibernate auto-update enabled

---

## 🛠️ Development Setup

### Prerequisites
- **Java**: JDK 17
- **Node.js**: Latest LTS version
- **Maven**: 3.6+
- **MySQL**: 8.0+
- **IDE**: IntelliJ IDEA / Eclipse / VS Code

### Backend Setup
```bash
cd VIM-BE
mvn clean install
mvn spring-boot:run
```

### Frontend Setup
```bash
cd VIM-FE
npm install
ng serve
```

### Database Setup
1. Create MySQL database: `vim_3`
2. Update `application.properties` with credentials
3. Run SQL scripts from `VIM-BE/` directory
4. Application will auto-create tables on first run

---

## 📊 Key Statistics

- **Backend Controllers**: 40+
- **Frontend Components**: 391+ TypeScript files
- **HTML Templates**: 155+
- **Database Entities**: 100+ tables
- **API Endpoints**: 200+ REST endpoints
- **Supported Languages**: 15+ (i18n)

---

## 🔗 External Integrations

1. **SAP ERP System**
   - SOAP web services
   - E-Invoice processing
   - Workflow integration

2. **Email Service (Gmail SMTP)**
   - Notification emails
   - Approval workflows
   - Password reset

3. **File Storage**
   - Local file system
   - Document uploads
   - Export functionality

---

## 📝 Notes

- **Application Name**: VIM (Vendor Invoice Management)
- **Project Structure**: Monorepo (Frontend + Backend)
- **Version Control**: Git
- **Build Tool**: Maven (Backend), npm (Frontend)
- **Package Manager**: npm/yarn (Frontend)

---

## 🚀 Future Enhancements

Potential areas for improvement:
- Microservices architecture migration
- Docker containerization
- Kubernetes deployment
- Redis caching layer
- Elasticsearch for advanced search
- WebSocket for real-time updates
- GraphQL API alternative

---

*Last Updated: Based on current codebase analysis*
*Documentation Version: 1.0*

